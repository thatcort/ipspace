/*
 * Created on 7-Jan-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;

import com.liminal.ipspace.IPSpace;
import com.liminal.ipspace.whois.GetNetInfoResult;
import com.liminal.ipspace.whois.GetSubnetResult;
import com.liminal.ipspace.whois.RIRDelegationResult;
import com.liminal.ipspace.whois.WhoisManager;
import com.liminal.ipspace.whois.WhoisManager.Source;


public class DataManager {

	private static Logger LOG = Logger.getLogger(DataManager.class.getName());
	
	private Net internet;
	
	private WhoisManager whoisMgr;
	
	private NetTree netTree;
//	private Map<Net, Net[]> subnetMap = Collections.synchronizedMap(new FastMap<Net, Net[]>());
	
	private Map<Net, String> netInfoMap = Collections.synchronizedMap(new FastMap<Net, String>());
	
	private List<Future<RIRDelegationResult>> rirDelegationFutures = new LinkedList<Future<RIRDelegationResult>>();
	
	private LinkedHashMap<Net, SubnetFuture> subnetFutures = new LinkedHashMap<Net, SubnetFuture>();
	
	private LinkedHashMap<Net, NetInfoFuture> netInfoFutures = new LinkedHashMap<Net, NetInfoFuture>();
	
//	private LinkedHashMap<Net, HostInfoFuture> hostInfoFutures = new LinkedHashMap<Net, HostInfoFuture>();
	
	private final boolean storeNets;
	
	public DataManager() throws Throwable {
		this(true);
	}
	public DataManager(boolean storeNets) throws Throwable {
		this.storeNets = storeNets;
		
		try {
			whoisMgr = new WhoisManager();
			
			init();
		} catch (Throwable ex) {
			LOG.log(Level.SEVERE, "Problem initializing. Shutting down.", ex);
			shutdown();
			throw ex;
		}
	}
	
	public void shutdown() {
		whoisMgr.shutdown();
	}

	
	private void init() throws Exception {
		Net[] roots = null;
		roots = whoisMgr.initRoots();
		
		internet = whoisMgr.getInternet();
		netTree = new NetTree(internet);


		if (roots != null) // will be null in case of historical mode
			storeSubnets(internet, roots);
		
		if (IPSpace.getMode() != IPSpace.Mode.RFC997) {
			// initiate the loading of the rir delegation results:
			Future<RIRDelegationResult>[] futures = whoisMgr.initRIRs(roots);
			synchronized (rirDelegationFutures) {
				for (Future<RIRDelegationResult> f : futures)
					rirDelegationFutures.add(f);
			}
		}
	}
	
	private void processRIRResults(RIRDelegationResult[] results) {
		for (RIRDelegationResult result : results) {
			Net[] nets = result.getNets();
			netTree.addNets(nets);
		}
	}

	public Net getInternet() { return internet; }
	
	public String getNetInfo(Net net) {
		if (IPSpace.getMode() == IPSpace.Mode.CURRENT)
			processFutures();
		
		if (!net.isAllocated())
			return net.getRange().toString() + '\n' + net.getRegistry() + "\nUnallocated";
		
		if (net == internet)
			return "IPv4 Internet";

		if (IPSpace.getMode() != IPSpace.Mode.CURRENT)
			return net.getId() + '\n' + net.getRange().toString();
		
		String info = netInfoMap.get(net);
		if (info == null) {
			synchronized (netInfoFutures) {
				if (!netInfoFutures.containsKey(net)) {
					Future<GetNetInfoResult> future = whoisMgr.getNetInfo(net, IPSpace.VALID_SOURCES);
					NetInfoFuture nif = new NetInfoFuture(future);
					netInfoFutures.put(net, nif);
				}
			}
		}
		return info;
	}
	
//	public void getHostInfo(HostNet net) {
//		if (IPSpace.getMode() == IPSpace.Mode.CURRENT)
//			processFutures();
//		
//		if (!net.isHost())
//			throw new IllegalArgumentException("Attempt to get host info for network range " + net.getRange().toString());
//		
//		synchronized (hostInfoFutures) {
//			Future<GetHostInfoResult> future = whoisMgr.getHostInfo(net);
//			HostInfoFuture hif = new HostInfoFuture(future);
//			hostInfoFutures.put(net, hif);
//		}
//	}

	/** Returns whether the request could be cancelled */
	public boolean cancelNetInfoRequest(Net net) {
		synchronized(netInfoFutures) {
			NetInfoFuture nif = netInfoFutures.get(net);
			if (nif != null) {
System.out.println("Cancelling net info request for: " + net.getRange());
				boolean cancelled = nif.future.cancel(true);
				if (cancelled)
					netInfoFutures.remove(net);
				return cancelled;
			}
			return false;
		}
	}

	
	/** 
	 * Will query for the nets if we don't have any yet 
	 * @return An array of Nets if we've queries for them and received results (the array might be 0-length if none were found) or null if we don't know yet and are performing query
	 */
	public Net[] getSubnets(Net net) {
		return getSubnets(net, IPSpace.VALID_SOURCES);
	}
	
	/** Returns whether the request could be cancelled */
	public boolean cancelSubnetRequest(Net net) {
		synchronized(subnetFutures) {
System.out.println("Cancelling subnet request for: " + net.getRange());
			SubnetFuture sf = subnetFutures.get(net);
			if (sf != null) {
				boolean cancelled = sf.future.cancel(true);
				if (cancelled)
					subnetFutures.remove(net);
				return cancelled;
			}
			return false;
		}
	}
	
	/** should really only be called once isFinishedProcessingRirDelegations() returns true 
	 * @throws Exception */
	public Net[] getSubnetsBlocking(Net net) throws Exception {
		return getSubnetsBlocking(net, IPSpace.VALID_SOURCES);
	}
	public Net[] getSubnetsBlocking(Net net, Set<Source> sources) throws Exception {
		processFutures();

		if (!net.isAllocated())
			return new Net[0];

		Net[] subnets = getStoredSubnets(net);
		if (subnets == null) {
			if (net != internet && !rirDelegationFutures.isEmpty())
				return null;
			
			if (IPSpace.getMode() != IPSpace.Mode.CURRENT)
				return new Net[0];
			
			GetSubnetResult gsr = whoisMgr.getSubnetsBlocking(net, sources);
			subnets = gsr.getSubnets();
			if (subnets == null)
				return null; // connection problem? server reset?
			Arrays.sort(subnets); // must sort, since might not be sorted by the nettree
			
			if (storeNets)
				storeSubnets(net, subnets);
			
		}
		return subnets;
	}
	
	/**
	 * Will query for the nets if we don't have any yet, and will later ensure that the results are 
	 * checked to ensure that the subnet ranges exactly match the resultRanges passed into this method.
	 * Will not process pending futures.
	 * @param net
	 * @param sources The sources to load the data from
	 * @return the subnets if we already have them. Null if an operation to load them has been initiated in another thread.
	 */
	public Net[] getSubnets(Net net, Set<Source> sources) {
		processFutures();
		
		if (!net.isAllocated())
			return new Net[0];
		
		Net[] subnets = getStoredSubnets(net);
		if (subnets == null) {
			
			// don't initiate any subnet gets until we've processed all the rir delegation files
			if (net != internet && !rirDelegationFutures.isEmpty())
				return null;

			if (IPSpace.getMode() != IPSpace.Mode.CURRENT)
				return new Net[0];

			synchronized (subnetFutures) {
				if (!subnetFutures.containsKey(net)) {
					Future<GetSubnetResult> future = whoisMgr.getSubNets(net, sources);
					SubnetFuture sf = new SubnetFuture(future);
					subnetFutures.put(net, sf);
				}
			}
		}
		return subnets;
		
	}
	
	public boolean hasStoredSubnets(Net net) {
		processFutures();
		return netTree.hasSubnets(net);
	}
	
	private Net[] getStoredSubnets(Net net) {
		return netTree.getSubnets(net);
	}
	
	private void storeSubnets(Net parent, Net[] children) {
		if (children.length > 0)
			netTree.addNets(children);
		else
			netTree.setNoChildren(parent);
	}
	
	public void tick() {
		processFutures();
	}
	
	public int getNumPendingRequests() {
		return rirDelegationFutures.size() + subnetFutures.size() + netInfoFutures.size();
	}
	
	public boolean isFinishedProcessingRirDelegations() {
		if (rirDelegationFutures.isEmpty())
			return true;
		for (Iterator<Future<RIRDelegationResult>> iter = rirDelegationFutures.iterator(); iter.hasNext(); ) {
			Future<RIRDelegationResult> f = iter.next();
			if (!f.isDone()) {
				return false;
			}
		}
		return true;
	}
	
	private void processFutures() {
//System.out.println("Process Futures: Thread: " + Thread.currentThread().toString());
		// check for any RIR delegation results, but wait to process them all at once
		synchronized (rirDelegationFutures) {
			boolean allDone = isFinishedProcessingRirDelegations();

			// waiting until they are all done allows us to make sure none of them overlap when we are processing them
			if (allDone && !rirDelegationFutures.isEmpty()) {
				ArrayList<RIRDelegationResult> results = new ArrayList<RIRDelegationResult>(rirDelegationFutures.size());
				for (Iterator<Future<RIRDelegationResult>> iter = rirDelegationFutures.iterator(); iter.hasNext(); ) {
					Future<RIRDelegationResult> f = iter.next();
					if (f.isDone()) {
						try {
							iter.remove();
							results.add(f.get());
						} catch (Throwable t) {
							LOG.log(Level.WARNING, "Problem processing RIR results.", t);
						}
					}
				}
				processRIRResults(results.toArray(new RIRDelegationResult[results.size()]));
			}
		}
		
		// don't initiate any subnet gets until we've processed all the rir delegation files
		if (!rirDelegationFutures.isEmpty())
			return;
		
		// handle subnet query results
		synchronized (subnetFutures) {
			for (Iterator<Entry<Net, SubnetFuture>> iter = subnetFutures.entrySet().iterator(); iter.hasNext(); ) {
				Entry<Net, SubnetFuture> entry = iter.next();
				SubnetFuture sf = entry.getValue();

				Future<GetSubnetResult> f = sf.future;
				if (f.isDone()) {
					try {
						iter.remove();
						GetSubnetResult result = f.get();
						Net parent = result.getNet();
						Net[] subnets = result.getSubnets();
						
						if (subnets != null) {
							System.out.println(parent.getRange() + " has " + subnets.length + " subnets.");
							if (storeNets)
								storeSubnets(parent, subnets);
						} else if (result.getNet().getLevel() == 1) {
							// since its parent is a root (level 1) if it had been assigned, 
							// it would have been in the rir delegation results (which we've already parsed)
							// since it is null, it must therefore not have any subnets
							storeSubnets(parent, new Net[0]);
						}
						
					} catch (Throwable t) {
						LOG.log(Level.WARNING, "Problem processing subnet futures for parent net " + entry.getKey(), t);
//						String msg = e.getMessage();
//						if (msg.indexOf("Unknown registry name") == -1)
//							e.printStackTrace();
					}
				}
			}
		}
		
		// handle net info query results
		synchronized (netInfoFutures) {
			for (Iterator<Entry<Net, NetInfoFuture>> iter = netInfoFutures.entrySet().iterator(); iter.hasNext(); ) {
				Entry<Net, NetInfoFuture> entry = iter.next();
				NetInfoFuture nif = entry.getValue();

				Future<GetNetInfoResult> f = nif.future;
				if (f.isDone()) {
					try {
						iter.remove();
						GetNetInfoResult result = f.get();
						Net net = result.getNet();
						String info = result.getInfo();
						
						if (info != null) {
							netInfoMap.put(net, info);
						}
					} catch (Exception ex) {
						LOG.log(Level.WARNING, "Problem processing net info futures for net " + entry.getKey(), ex);
					}
				}
			}
		}
		
//		// handle host info query results
//		synchronized (hostInfoFutures) {
//			for (Iterator<Entry<Net, HostInfoFuture>> iter = hostInfoFutures.entrySet().iterator(); iter.hasNext(); ) {
//				Entry<Net, HostInfoFuture> entry = iter.next();
//				HostInfoFuture hif = entry.getValue();
//
//				Future<GetHostInfoResult> f = hif.future;
//				if (f.isDone()) {
//					try {
//						iter.remove();
//						GetHostInfoResult result = f.get();
//						HostNet net = result.getNet();
//						net.setHostInfo(result);
//					} catch (Exception ex) {
//						LOG.log(Level.WARNING, "Problem getting host info for " + entry.getKey(), ex);
//					}
//				}
//			}
//		}
	}
	
	private class SubnetFuture {
		Future<GetSubnetResult> future;
		public SubnetFuture(Future<GetSubnetResult> future) {
			this.future = future;
		}
	}
	
	private class NetInfoFuture {
		Future<GetNetInfoResult> future;
		public NetInfoFuture(Future<GetNetInfoResult> future) {
			this.future = future;
		}
	}
	
//	private class HostInfoFuture {
//		Future<GetHostInfoResult> future;
//		public HostInfoFuture(Future<GetHostInfoResult> future) {
//			this.future = future;
//		}
//	}
	
	
	public static void main(String[] args) throws Throwable {
		DataManager dm = null;
		try {
			dm = new DataManager();
			
//			Net[] roots = dm.getSubnets(dm.internet);
//	System.out.println(roots.length + " class A networks.");
//			for (int i=0; i < roots.length; i++) {
//				dm.getSubnets(roots[i]);
//			}
		} finally {
			if (dm != null)
				dm.shutdown();
		}
	}
}
