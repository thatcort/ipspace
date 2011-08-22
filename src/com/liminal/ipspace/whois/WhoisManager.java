/*
 * Created on 2-Jan-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import static com.liminal.ipspace.data.Registry.AFRINIC;
import static com.liminal.ipspace.data.Registry.APNIC;
import static com.liminal.ipspace.data.Registry.ARIN;
import static com.liminal.ipspace.data.Registry.IANA;
import static com.liminal.ipspace.data.Registry.LACNIC;
import static com.liminal.ipspace.data.Registry.RIPE;
import static com.liminal.ipspace.data.Registry.JPNIC;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ConnectException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;

import au.com.bytecode.opencsv.CSVReader;

import com.liminal.ipspace.IPSpace;
import com.liminal.ipspace.data.Address;
import com.liminal.ipspace.data.AddressRange;
import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.data.Registry;

public class WhoisManager {

	private static Logger LOG = Logger.getLogger(WhoisManager.class.getName());
	
	private static final String DNS = "DNS"; // dns and reverse-dns lookup
	
	HashMap<Registry, WhoisClient2> registryClientMap = new HashMap<Registry, WhoisClient2>();
	private HashMap<Registry, ExecutorService> registryExecutorMap = new HashMap<Registry, ExecutorService>();
	private ExecutorService localExec = Executors.newSingleThreadExecutor();
	
	private WhoisDB2 db;
	
	private Net internet;
	
	public static enum Source {
		LOCAL, // only request from the local db
		REMOTE // will only request the most up to date info from the remove public whois server
	}
	
	/** The standard source wll first check the local db, then if not found will check the public whois databases */
	public static final Set<Source> BOTH_SOURCES = (Set<Source>) Collections.unmodifiableSet(EnumSet.of(Source.LOCAL, Source.REMOTE));
	public static final Set<Source> LOCAL_ONLY_SOURCE = (Set<Source>) Collections.unmodifiableSet(EnumSet.of(Source.LOCAL));
	public static final Set<Source> REMOTE_ONLY_SOURCE = (Set<Source>) Collections.unmodifiableSet(EnumSet.of(Source.REMOTE));
	
	
	public WhoisManager() throws ClassNotFoundException, SQLException {
//		internet = new Net(null, new AddressRange(new Address(0,0,0,0), new Address(255,255,255,255)), "IPv4 Internet", IANA, "INTERNET", "IANA");
		
		if (IPSpace.getMode() == IPSpace.Mode.CURRENT) {
//			db = new PooledWhoisDB();
//			db = new SingleConnectionWhoisDB();
//			db = new SingleConnectionPreparedStatementWhoisDB();
			db = new WhoisDB2H2();
		}

		register(IANA, null);
		register(ARIN, new ArinClient2(db));
		register(RIPE, new InternationalClient2(RIPE, db));
		register(APNIC, new InternationalClient2(APNIC, db));
		register(AFRINIC, new InternationalClient2(AFRINIC, db));
		register(LACNIC, new LacnicClient2(db));
		register(JPNIC, new JpnicClient2(db));
	}
	
	public void shutdown() {
		localExec.shutdown();
		for (ExecutorService exec : registryExecutorMap.values()) {
			exec.shutdown();
		}
		try {
			db.shutdown();
		} catch (SQLException e1) {
			LOG.log(Level.WARNING, "Problem shutting down database.", e1);
		}
		try {
			for (ExecutorService exec : registryExecutorMap.values()) {
				exec.awaitTermination(10, TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
			LOG.warning("Interrupted waiting for WhoisManager Executor shutdown.");
		}
	}
	
	
	void register(Registry registry, WhoisClient2 client) {
		registryClientMap.put(registry, client);
//		if (client != null)
	}
	
	public Net getInternet() { return internet; }
	
	
	/**
	 * Loads the top level nets. Does it synchronously -- blocks!
	 * @param internet
	 * @param url location of the list of root id's
	 * @return the top level subnets
	 */
	public Net[] initRoots() throws Exception {
		if (IPSpace.getMode() == IPSpace.Mode.CURRENT) {
			// bootstrap the internet
			String dataDir = IPSpace.getCommandLineArgValue("datadir");
			if (dataDir == null)
				dataDir = "data";
			URL url = new File(dataDir + "/init.csv").toURL();
			Reader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			CSVReader csv = new CSVReader(reader);
			csv.readNext(); // skip the headers
			
			FastMap<String, Net> handleNetMap = new FastMap<String, Net>();
			
			ArrayList<Net> rootList = new ArrayList<Net>();
			String[] line;
			while ((line = csv.readNext()) != null) {
				String netHandle = line[0];
				String regStr = line[1];
				String parentHandle = line[2];
				// the rest are ignored
					
				Net parent = null;
//				if (parentHandle == null || parentHandle.trim().length() == 0)
//					parent = internet;
//				else
					parent = handleNetMap.get(parentHandle);
				
				Registry registry = Registry.getRegistryByName(regStr);
				WhoisClient2 client = registryClientMap.get(registry);
				
				GetNetResult gnr = null;
				if (IPSpace.VALID_SOURCES.contains(Source.LOCAL))
					gnr = client.getNet(parent, netHandle, Source.LOCAL);
				if (gnr.getNet() == null && IPSpace.VALID_SOURCES.contains(Source.REMOTE))
					gnr = client.getNet(parent, netHandle, Source.REMOTE);
				Net net = gnr.getNet();
				if (net != null) {
					handleNetMap.put(netHandle, net);
					rootList.add(net);
				}
			}
			
			internet = rootList.get(0);
			
			Net[] roots = rootList.subList(1, rootList.size()).toArray(new Net[0]);
			
			
			return roots;
		} else if (IPSpace.getMode() == IPSpace.Mode.HISTORICAL) {
			internet = new Net(null, new AddressRange(new Address(0,0,0,0), new Address(255,255,255,255)), "IPv4 Internet", IANA, "INTERNET", "IANA");
			return null;
		} else if (IPSpace.getMode() == IPSpace.Mode.RFC997) {
			internet = new Net(null, new AddressRange(new Address(0,0,0,0), new Address(255,255,255,255)), "IPv4 Internet", IANA, "INTERNET", "IANA");
			URL url = new File("data/rfc997.csv").toURL();
			RFC997Reader rfc997Reader = new RFC997Reader(internet, url);
			return rfc997Reader.readIPv4Nets();
		}
		return null; // shouldn't happen
//		if (IPSpace.getMode() == IPSpace.Mode.CURRENT) {
//			// bootstrap the internet
//			String dataDir = IPSpace.getCommandLineArgValue("datadir");
//			if (dataDir == null)
//				dataDir = "data";
//			URL url = new File(dataDir + "/roots.csv").toURL();
//			Reader reader = new BufferedReader(new InputStreamReader(url.openStream()));
//			CSVReader csv = new CSVReader(reader);
//			csv.readNext(); // skip the headers
//			
//			WhoisClient2 arinClient = registryClientMap.get(ARIN);
//			
//			ArrayList<Net> rootList = new ArrayList<Net>();
//			String[] line;
//			while ((line = csv.readNext()) != null) {
//				//			String netName = line[0];
//				String netHandle = line[1];
//				//			String start = line[2];
//				//			String end = line[3];
//				//			String registry = line[4];
//				//			String date = line[5];
//				//			String purpose = line[6];
//				
//				GetNetResult gnr = null;
//				if (IPSpace.VALID_SOURCES.contains(Source.LOCAL))
//					gnr = arinClient.getNet(internet, netHandle, Source.LOCAL);
//				if (gnr.getNet() == null && IPSpace.VALID_SOURCES.contains(Source.REMOTE))
//					gnr = arinClient.getNet(internet, netHandle, Source.REMOTE);
//				Net net = gnr.getNet();
//				if (net != null)
//					rootList.add(net);
//			}
//			Net[] roots = rootList.toArray(new Net[rootList.size()]);
//			return roots;
//		} else if (IPSpace.getMode() == IPSpace.Mode.RFC997) {
//			URL url = new File("data/rfc997.csv").toURL();
//			RFC997Reader rfc997Reader = new RFC997Reader(internet, url);
//			return rfc997Reader.readIPv4Nets();
//		}
//		return null; // shouldn't happen
	}
	
	/** In historical mode, roots will be null and can be ignored */
	public Future<RIRDelegationResult>[] initRIRs(Net[] roots) {
		Registry[] registries = new Registry[] { ARIN, AFRINIC, APNIC, RIPE, LACNIC };
		
		Future<RIRDelegationResult>[] futures = new Future[registries.length];
		
		for (int i=0; i < registries.length; i++) {
			ExecutorService exec = getExecutorService(registries[i]);
			futures[i] = exec.submit(new GetRIRDelegationsCallable(registries[i], roots, IPSpace.VALID_SOURCES));
		}
		
		return futures;
	}

	// TODO: enhance to cache the delegation files, add a source option, and a date property to the request & reponse
	private class GetRIRDelegationsCallable implements Callable<RIRDelegationResult> {
		Registry registry;
		Net[] roots;
		Set<Source> sources;
		GetRIRDelegationsCallable(Registry registry, Net[] roots, Set<Source> sources) {
			this.registry = registry;
			this.roots = roots;
			this.sources = sources;
		}
		public RIRDelegationResult call() throws Exception {
			AddressRange[] ranges = new RIRStatisticsHandler(registry, sources).loadResults();

			Net[] nets;
			if (IPSpace.getMode() != IPSpace.Mode.CURRENT) {
				nets = new HistoricalRIRProcessor().processRIRDelegations(registry, ranges, internet);
			} else {
				if (registry.equals(ARIN)) {
					// arin can't be searched by address range, so we need to find the id for each of the returned ranges
					nets = new RIRProcessor(WhoisManager.this).processArinRIRDelegations(ranges, roots, sources);
				} else {
					nets = new RIRProcessor(WhoisManager.this).processNonArinRIRDelegations(registry, ranges, roots);
				}
			}
//			Arrays.sort(nets);
			LOG.fine("Completed RIR Delegations Callable: " + registry);
			return new RIRDelegationResult(registry, nets);
		}
	}

	
	
	public Future<GetSubnetResult> getSubNets(Net net) {
		return getSubNets(net, IPSpace.VALID_SOURCES);
	}
	
	public Future<GetSubnetResult> getSubNets(Net net, Set<Source> sources) {
		return new GetSubnetsFuture(net, sources);
	}
	
	public GetSubnetResult getSubnetsBlocking(Net net) throws Exception {
		return getSubnetsBlocking(net, IPSpace.VALID_SOURCES);
	}
	public GetSubnetResult getSubnetsBlocking(Net net, Set<Source> sources) throws Exception {
		GetSubnetResult result = null;
		if (sources.contains(Source.LOCAL))
			result = getSubnetsBlocking(net, Source.LOCAL);
		if (result.getSubnets() == null && sources.contains(Source.REMOTE))
			result = getSubnetsBlocking(net, Source.REMOTE);
		return result;
	}
	
	public GetSubnetResult getSubnetsBlocking(Net net, Source source) throws Exception {
		GetSubnetsCallable2 callable = new GetSubnetsCallable2(net, source);
		return callable.call();
	}
	


	
	private abstract class NetCallable2<T> implements Callable<T> {
		Net net;
		Source source;
		NetCallable2(Net net, Source source) {
			this.net = net;
			this.source = source;
		}
		abstract Registry getRegistry();
		public Source getSource() { return source; }
	}
	
	
	private class GetSubnetsCallable2 extends NetCallable2<GetSubnetResult> {
		private GetSubnetsFuture future;
		GetSubnetsCallable2(Net net, Source source) {
			this(null, net, source);
		}
		GetSubnetsCallable2(GetSubnetsFuture future, Net net, Source source) {
			super(net, source);
			this.future = future;
		}
		Registry getRegistry() {
			return net.getRegistry();
		}
		public GetSubnetResult call() throws Exception {
			GetSubnetResult returnResult = null;
			Exception exception = null;
			
			if (getRegistry().equals(IANA))
				returnResult = new GetSubnetResult(net, new Net[0], Source.LOCAL);
			
			if (returnResult == null) {
				try {
					WhoisClient2 client = registryClientMap.get(getRegistry());
					if (client != null) {
						try {
							GetSubnetResult result = client.getSubnets(net, source);
							returnResult = new GetSubnetResult(net, result.getSubnets(), result.getResponseSource());
						} catch (ConnectException connex) {
							// thrown by arin when we're no longer allowed to connect
							LOG.log(Level.WARNING, "Problem connecting to " + client.getServer() + ". Removing server from list of registries.", connex);
							registryClientMap.remove(client);
							throw connex;
						}
					} else 
						throw new Exception("Unknown registry name: " + getRegistry() + " for subnet request on " + net.getRange());
				} catch (Exception ex) {
					exception = ex;
				}
			}
			
			if (future != null)
				future.setResult(returnResult, exception);
			
			if (exception != null)
				throw exception;
			
			return returnResult;
		}
	}
	
	

	private class GetSubnetsFuture implements Future<GetSubnetResult> {
		private final Net net;
		private final Set<Source> sources;
		private volatile Future<GetSubnetResult> future;
		private volatile GetSubnetResult result;
		private volatile Exception exception;
		private volatile boolean cancelled = false;
		
		GetSubnetsFuture(Net net) {
			this(net, IPSpace.VALID_SOURCES);
		}
		GetSubnetsFuture(Net net, Set<Source> sources) {
//			super(new GetSubnetsCallable2(this, net, sources));
			this.net = net;
			this.sources = sources;
			GetSubnetsCallable2 callable = null;
			if (net.getRegistry() == IANA) {
				result = new GetSubnetResult(net, new Net[0], Source.LOCAL);
			} else if (sources.contains(Source.LOCAL)) {
				callable = new GetSubnetsCallable2(this, net, Source.LOCAL);
			} else {
				callable = new GetSubnetsCallable2(this, net, Source.REMOTE);
			}
			if (callable != null) {
				ExecutorService exec = getExecutorService(callable);
				future = exec.submit(callable);
			}
		}
		synchronized void setResult(GetSubnetResult result, Exception exception) {
			while (future == null) {
//				System.out.println("GetNetInfoFuture::setResult waiting for constructor to finish...");
				try {
					Thread.sleep(100);
				} catch (InterruptedException iex) {
					/* ignore */
				}
			}
			this.result = result;
			this.exception = exception;
			future = null;
			if (!cancelled && exception == null && result.getSubnets() == null && result.getResponseSource() == Source.LOCAL && sources.contains(Source.REMOTE)) {
				result = null;
				GetSubnetsCallable2 callable = new GetSubnetsCallable2(this, net, Source.REMOTE);
				ExecutorService exec = getExecutorService(callable);
				future = exec.submit(callable);
			} else {
				notifyAll();
			}
		}
		public synchronized boolean cancel(boolean mayInterruptIfRunning) {
			cancelled = true;
			return future != null && future.cancel(mayInterruptIfRunning);
		}
		public synchronized GetSubnetResult get() throws InterruptedException, ExecutionException {
			while (future != null)
				wait();
			if (exception != null)
				throw new ExecutionException(exception);
			return result;
		}
		public synchronized GetSubnetResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			long millis = unit.toMillis(timeout);
			long nanos = unit.toNanos(timeout);
			nanos -= millis * 1000000;
			while (future != null)
				wait(millis, (int) nanos);
			if (exception != null)
				throw new ExecutionException(exception);
			return result;
		}
		public synchronized boolean isCancelled() {
			return cancelled;
		}
		public synchronized boolean isDone() {
			return future == null;
		}
	}
	
	
	
	
	private class GetNetInfoFuture implements Future<GetNetInfoResult> {
		private final Net net;
		private final Set<Source> sources;
		private volatile Future<GetNetInfoResult> future;
		private volatile GetNetInfoResult result;
		private volatile Exception exception;
		private volatile boolean cancelled = false;
		
		GetNetInfoFuture(Net net) {
			this(net, IPSpace.VALID_SOURCES);
		}
		GetNetInfoFuture(Net net, Set<Source> sources) {
			this.net = net;
			this.sources = sources;
			GetNetInfoCallable callable = null;
			if (sources.contains(Source.LOCAL)) {
				callable = new GetNetInfoCallable(this, net, Source.LOCAL);
			} else {
				callable = new GetNetInfoCallable(this, net, Source.REMOTE);
			}
			if (callable != null) {
				ExecutorService exec = getExecutorService(callable);
				future = exec.submit(callable);
//System.out.println("GetNetInfoFuture<init> " + this + " future: " + future);
			}
		}
		synchronized void setResult(GetNetInfoResult result, Exception exception) {
			while (future == null) {
//				System.out.println("GetNetInfoFuture::setResult waiting for constructor to finish...");
				try {
					Thread.sleep(100);
				} catch (InterruptedException iex) {
					/* ignore */
				}
			}
			this.result = result;
			this.exception = exception;
			future = null;
			this.future = null;
//System.out.println("setting info result for " + this + " " + result.getNet().getRange() + ": hasResult=" + (result.getInfo() != null) + " exception=" + (exception != null) + " source="+result.getResponseSource() + " future=" + future);
			
			if (!cancelled && exception == null && result.getInfo() == null && result.getResponseSource() == Source.LOCAL && sources.contains(Source.REMOTE)) {
//System.out.println("making remote info call for  " + this + " " + result.getNet().getRange());
				result = null;
				GetNetInfoCallable callable = new GetNetInfoCallable(this, net, Source.REMOTE);
				ExecutorService exec = getExecutorService(callable);
				future = exec.submit(callable);
			} else {
				notifyAll();
			}
		}
		public synchronized boolean cancel(boolean mayInterruptIfRunning) {
			cancelled = true;
			return future != null && future.cancel(mayInterruptIfRunning);
		}
		public synchronized GetNetInfoResult get() throws InterruptedException, ExecutionException {
			while (future != null)
				wait();
			if (exception != null)
				throw new ExecutionException(exception);
			return result;
		}
		public synchronized GetNetInfoResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			long millis = unit.toMillis(timeout);
			long nanos = unit.toNanos(timeout);
			nanos -= millis * 1000000;
			while (future != null)
				wait(millis, (int) nanos);
			if (exception != null)
				throw new ExecutionException(exception);
			return result;
		}
		public synchronized boolean isCancelled() {
			return cancelled;
		}
		public synchronized boolean isDone() {
//System.out.println("getNetInfoFuture::isDone " + this + " future: " + future);
			return future == null;
		}
	}
	

	
	
	public Future<GetNetInfoResult> getNetInfo(Net net) {
		return getNetInfo(net, IPSpace.VALID_SOURCES);
	}
	
	public Future<GetNetInfoResult> getNetInfo(Net net, Set<Source> sources) {
		return new GetNetInfoFuture(net, sources);
	}
	
	private class GetNetInfoCallable extends NetCallable2<GetNetInfoResult> {
		private GetNetInfoFuture future;
		GetNetInfoCallable(GetNetInfoFuture future, Net net, Source source) {
			super(net, source);
			this.future = future;
		}
		Registry getRegistry() {
			Registry registry = null;
			Net parent = net.getParent();
			if (parent == null)
				return IANA;
			if (parent.getRegistry() == ARIN) {
				// if the child has an id that can't be parsed as an address range, then assume its an arin id
				if (net.getRegistry() == ARIN) {
					registry = ARIN;
				} else {
					try {
						AddressRange.parseAddressRange(net.getId());
						registry = net.getRegistry();
					} catch (Throwable t) {
						registry = ARIN;
					}
				}
			} else if (parent.getRegistry() == IANA) {
				registry = ARIN;
			} else {
				registry = parent.getRegistry();
			}
			return registry;
//			String registry = null;
//			Net parent = net.getParent();
//			if (parent != null) {
//				registry = parent.getRegistry();
//				if (registry.equals(IANA))
//					registry = ARIN;
//			} else {
//				// only the whole internet has a null parent and we know that ripe has this in its database
//				registry = RIPE;
//			}
//			return registry;
//			return net.getRegistry();
		}
		public GetNetInfoResult call() throws Exception {
			Exception exception = null;
			GetNetInfoResult result = null;
			try {
				Registry registry = getRegistry();
				WhoisClient2 client = registryClientMap.get(registry);
				if (client != null) {
					GetNetInfoResult gnir = client.getNetInfo(net, source);
	
					String info = gnir.getInfo();
					if (info != null)
						info += "\nSource: " + client.getServer();
					result = new GetNetInfoResult(net, info, gnir.getResponseSource());
				} else { 
					throw new Exception("Unknown registry name: " + registry + " for net info request on " + net.getRange());
				}
			} catch (Exception ex) {
				exception = ex;
			}
			
			future.setResult(result, exception);
			
			if (exception != null)
				throw exception;
			
			return result;
			
		}
	}
	

	
	
	private ExecutorService getExecutorService(NetCallable2 netCallable) {
		if (netCallable.source == Source.LOCAL)
			return localExec;
		Registry registry = netCallable.getRegistry();
		return getExecutorService(registry);
	}
	
	private ExecutorService getExecutorService(Registry registry) {
		ExecutorService exec = registryExecutorMap.get(registry);
		if (exec == null) {
			exec = Executors.newSingleThreadExecutor();
			registryExecutorMap.put(registry, exec);
		}
		return exec;
	}

}
