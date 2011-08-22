package com.liminal.ipspace;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.liminal.ipspace.data.DataManager;
import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.whois.WhoisManager;

public class PDFGenerator {

	private static Logger LOG;
	
	private static class SynchronousDataManager extends DataManager {

		public SynchronousDataManager(boolean storeNets) throws Throwable {
			super(storeNets);
		}

		@Override
		public Net[] getSubnets(Net net) {
			try {
				return super.getSubnetsBlocking(net);
			} catch (Exception e) {
				LOG.log(Level.WARNING, "Problem getting subnets in blocking call.", e);
			}
			return null;
		}
		
	}
	
	private static class NoLoopIPSpace extends IPSpace {

		public NoLoopIPSpace(DataManager dataMgrParam) throws Throwable {
			super(dataMgrParam);
		}

		@Override
		public void setup() {
			super.setup();
			noLoop();
		}
		
		public volatile boolean ignoreDraw = true;
		
		@Override
		public void draw() {
			if (ignoreDraw)
				return;
			super.draw();
			
			exit();
		}
	}
	
	public static void main(String[] args) throws Throwable {
		IPSpace.setMode(IPSpace.Mode.CURRENT);
		IPSpace.VALID_SOURCES = WhoisManager.BOTH_SOURCES;
		IPSpace.parseCommandLineArgs(args);
	
		LOG = Logger.getLogger(PDFGenerator.class.getName());
		
		DataManager dm = new SynchronousDataManager(false);
		
		NoLoopIPSpace ipspace = new NoLoopIPSpace(dm);
		
		while (!dm.isFinishedProcessingRirDelegations()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException iex) {
				/* ignore */
			}
		}
		System.out.println("DataManager finished processing rir delegation results. Continuing...");
		
		ipspace.init();
		try {
			Thread.sleep(5000);
		} catch (InterruptedException ex) { /* ignore */ }
		ipspace.overheadViewpoint();
//ipspace.setLayoutFunction(LayoutFunction.SIERPINSKI_KNOPP);
		ipspace.savePDF();
		ipspace.ignoreDraw = false;
		ipspace.redraw();
	}

}
