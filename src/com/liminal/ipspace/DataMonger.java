/*
 * Created on 6-Oct-06
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.liminal.ipspace.data.AddressRange;
import com.liminal.ipspace.data.DataManager;
import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.whois.WhoisManager;

public class DataMonger {
	
	private static Logger LOG;

	private static class SynchronousLocalDataManager extends DataManager {

		public SynchronousLocalDataManager(boolean storeNets) throws Throwable {
			super(storeNets);
		}

		@Override
		public Net[] getSubnets(Net net) {
			Net[] subnets = null;
			try {
				subnets =  super.getSubnetsBlocking(net, WhoisManager.LOCAL_ONLY_SOURCE);
			} catch (Exception e) {
				LOG.log(Level.WARNING, "Problem getting subnets in blocking call.", e);
			}
			if (subnets == null) {
				super.getSubnets(net, WhoisManager.REMOTE_ONLY_SOURCE);
			}
			return subnets;
		}
		
	}

	
	private IPSpace ipspace;
	private Net mongerNet;
	
	private AddressRange addressRange = null;
	
	private boolean mongerException = false;
	
	
//	private HashSet doneLevel1s = new HashSet()<
	
	public DataMonger(IPSpace ipspace) {
		this.ipspace = ipspace;
		
		if (IPSpace.getCommandLineArgValue("range") != null) {
			addressRange = AddressRange.parseAddressRange(IPSpace.getCommandLineArgValue("range"));
		}
	}
	
	public void start() {
		DataManager dm = ipspace.getDataManager();

		Thread.currentThread().setPriority(Thread.MIN_PRIORITY + 1);
		
		while (!ipspace.getDataManager().isFinishedProcessingRirDelegations()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException iex) {
				/* ignore */
			}
		}
		System.out.println("DataManager finished processing rir delegation results. Continuing...");
		
		Net mongerNet = dm.getInternet();
		
//AddressRange range192 = new AddressRange(new Address(192,1,1,1), new Address(192,2,2,2));
//AddressRange range198 = new AddressRange(new Address(198,1,1,1), new Address(198,2,2,2));
//AddressRange range221 = new AddressRange(new Address(221,1,1,1), new Address(221,2,2,2));
//Net[] level1 = dm.getInternet().getSubnets();
//for (int i=0; i < level1.length; i++) {
//	Net n = level1[i];
//	if (n.getRange().contains(range221)) {
//		mongerNet = n;
//System.out.println("MongerNet: " + mongerNet.getRange().toString());
//		break;
//	}
//}
		
		int counter = 0;
		boolean done = false;
		while (!done) {
			mongerException = false;
			
//			int pendingCount = monger(dm.getInternet());
			int pendingCount = monger(mongerNet);
			
			done = (pendingCount == 0);
			
			counter++;
//			if (counter % 100 == 0)
				System.out.println("Loop # " + counter + ". # Nets still pending: " + pendingCount);
			
			int dmPending; 
			while (!mongerException && (dmPending = dm.getNumPendingRequests()) > 0) {
				int sleepTime = 30;
				System.out.println("DataManager pending requests: " + dmPending + ". Sleeping " + sleepTime + " seconds.");
				try {
					Thread.sleep(sleepTime * 1000);
				} catch (InterruptedException iex) {
					/* ignore */
				}
				dm.tick();
			}
		}
		
		ipspace.getDataManager().shutdown();
		
		System.out.println("Done!");
		
	}
	
	/** returns # nets still remaining to do */
	private int monger(Net net) {
		if (net.getLevel() >= ipspace.getMinNetLevel())
			return 0;
		
		if (addressRange != null && !net.getRange().intersects(addressRange))
			return 0;
		
		Net[] subnets = null;
		try {
			subnets = net.getSubnets();
		} catch (Exception ex) {
			ex.printStackTrace();
			mongerException = true;
			return -1;
		}

		Thread.yield();
		
		if (subnets == null) {
//System.out.println("No info yet for: " + net.getRange().toString());
//try {
//	Thread.sleep(1000); // give the thread executor a bit of time to get going
//} catch (InterruptedException iex) {
//	/* ignore */
//}
			return 1;
		}

		int pendingCount = 0;
		for (int i=0; i < subnets.length; i++) {
			int count = monger(subnets[i]);
			if (count < 0 && mongerException)
				return pendingCount + 1;
			pendingCount += count;
		}
		if (pendingCount > 0)
			System.out.println(net.getRange().toString() + " has " + pendingCount + " pending subnet requests.");
		else if (net.getLevel() < 2)
			System.out.println(net.getRange().toString() + " complete!");
		
		return pendingCount;
	}
	
	
	public static void main(String[] args) throws Throwable {
		IPSpace.setMode(IPSpace.Mode.CURRENT);
		IPSpace.VALID_SOURCES = WhoisManager.BOTH_SOURCES;
		IPSpace.parseCommandLineArgs(args);

		LOG = Logger.getLogger(DataMonger.class.getName());
		
		DataManager dm = new DataManager(false);
//		SynchronousLocalDataManager dm = new SynchronousLocalDataManager(false);
		
		IPSpace ipspace = new IPSpace(dm);
		
		DataMonger monger = new DataMonger(ipspace);
		monger.start();
	}
}
