package com.liminal.ipspace;

import java.util.logging.Level;
import java.util.logging.Logger;

import processing.core.PConstants;
import processing.core.PGraphics3D;
import processing.pdf.PGraphicsPDF;

import com.liminal.ipspace.IPSpace.LayoutFunction;
import com.liminal.ipspace.data.DataManager;
import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.whois.WhoisManager;
import com.liminal.p5.util.Camera;
import com.liminal.p5.util.PickState;

public class HistoricalPDFGenerator {

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

		public void setHistoricalMaxDate(String date) throws Throwable {
			historicalMaxDate = date;
		}
		
		@Override
		public void setup() {
			size(printWidthPts, printHeightPts, P3D);
			colorScheme = new PDFColorScheme(this);
			
			float isize = HArea.getRoot().getLineDist();
			
			float isize2 = isize * .5f;
//	isize2 = 0f;
			
			float dist = isize2 / tan(PI / 3f); // * 2;

			camera = new Camera(this, isize2, -isize2, -dist, isize2, isize2, 0f, 0f, 0f, 1f);
			camera.roll(PI);
			camera.feed();
			
			background(colorScheme.getBackgroundColor());
			
			noLoop();
		}
		
		public volatile boolean ignoreDraw = true;
		
		public volatile boolean inDraw = false;

		@Override
		public synchronized void redraw() {
			inDraw = true;
			super.redraw();
		}
		
		@Override
		public void draw() {
			if (ignoreDraw)
				return;

			try {
//			super.draw();
				vinternet = null;
				drawMgr = null;
				dataMgr = null;
				System.gc();
					dataMgr = new SynchronousDataManager(false);
				vinternet = new VNet(null, dataMgr.getInternet());
				drawMgr = new DrawManager(this);
				drawMgr.setMinClosedNetLevel(minNetLevel);
	
				while (!dataMgr.isFinishedProcessingRirDelegations()) {
					try {
						dataMgr.tick();
						Thread.sleep(1000);
					} catch (InterruptedException iex) {
						/* ignore */
					}
				}
				System.out.println("DataManager finished processing rir delegation results. Continuing...");
				
	
	
				
				camera.feed();
				
				pickState = new PickState(this, camera);
	//			updateNearFarClippingPlanes();
				camera.feed();
				
				drawMgr.draw((PGraphics3D)g);

			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				inDraw = false;
			}
		}
		
	}
	
	private static LayoutFunction getLayoutFnArg() {
		String fv = IPSpace.getCommandLineArgValue("layout");
		if (fv != null)
			return fv.toLowerCase().startsWith("h") ? LayoutFunction.HILBERT : LayoutFunction.SIERPINSKI_KNOPP;
		return LayoutFunction.HILBERT;
	}
	

	private static int printWidthPts = 4 * 12 * 72;
	private static int printHeightPts;
	
	
	public static void main(String[] args) throws Throwable {
		IPSpace.setMode(IPSpace.Mode.HISTORICAL);
		IPSpace.VALID_SOURCES = WhoisManager.LOCAL_ONLY_SOURCE;
		IPSpace.parseCommandLineArgs(args);
		
		LOG = Logger.getLogger(HistoricalPDFGenerator.class.getName());
		
		String[] dates = new String[] {
				"20020101",
//				static String historicalMaxDate = "20030101"; // yyyymmdd e.g. 20050204
				"20040101",
//				static String historicalMaxDate = "20050101"; // yyyymmdd e.g. 20050204
				"20060101"
//				static String historicalMaxDate = "20070101"; // yyyymmdd e.g. 20050204
		};
		
		float marginPct = .08f;
		String marginStr = IPSpace.getCommandLineArgValue("marginpct");
		if (marginStr != null) {
			try {
				marginPct = Float.valueOf(marginStr);
			} catch (NumberFormatException nfe) {
				LOG.warning("Unable to parse float value for percent margin: " + marginStr + ". Using default value " + marginPct);
			}				
		}

		int margin = (int) (printWidthPts * marginPct);
		float netWidth = (printWidthPts - 4f * margin) / dates.length;
		printHeightPts = (int) Math.ceil((netWidth + 2 * margin)/72) * 72;
		float camDist = netWidth * .5f / (float) Math.tan(Math.PI/6);
		
		float strokeWeight = .1f;
		
		String fileName = "ipspaceHistorical_" + (getLayoutFnArg() == LayoutFunction.HILBERT ? 'H' : 'S') + "_"+ strokeWeight + "_" + System.currentTimeMillis() + ".pdf";
		System.out.println("Saving to file: " + fileName);
		
		DataManager dm = new SynchronousDataManager(false);
		
		NoLoopIPSpace ipspace = new NoLoopIPSpace(dm);
		
		ipspace.init();
		try {
			Thread.sleep(5000);
		} catch (InterruptedException ex) { /* ignore */ }
		
		PGraphicsPDF pdf = (PGraphicsPDF) ipspace.beginRaw(PConstants.PDF, fileName);
		System.out.println("saving pdf file: " + fileName);

		pdf.strokeJoin(PConstants.MITER);
		pdf.strokeWeight(strokeWeight);
		pdf.fill(0);
		pdf.rect(0, 0, printWidthPts, printHeightPts);
		
		for (int i=0; i < dates.length; i++) {
		
			pdf.pushMatrix();
			
			ipspace.setHistoricalMaxDate(dates[i]);

			float isize = HArea.getRoot().getLineDist();
			float isize2 = isize * .5f;
			float dist = isize2 / (ipspace.camera.getFOV() * .5f);
			dist *= 1.1f;

			float xlateDist = 1050;
			float camOffset = isize2;
			
			float vertOffset = -80f;
			
			pdf.translate(0f, -20f);
			
			if (i==0) {
				pdf.translate(-xlateDist, vertOffset);
				ipspace.camera.setTranslation(isize2 - camOffset, -isize2 * 1.2f, -dist);
				ipspace.camera.lookAt(isize2, isize2, 0f);		
			} else if (i==1) {
				dist *= 1.1f;
				ipspace.camera.setTranslation(isize2, -isize2 * 1.2f, -dist);
				ipspace.camera.lookAt(isize2, isize2, 0f);						
			} else if (i==2) {
				pdf.translate(xlateDist, vertOffset);
				ipspace.camera.setTranslation(isize2 + camOffset, -isize2 * 1.2f, -dist);
				ipspace.camera.lookAt(isize2, isize2, 0f);						
			}
				
//			float xlateDist = 1000;
//			float camOffset = isize2;
//			
//			float vertOffset = -80f;
//			
//			if (i==0) {
//				pdf.translate(-xlateDist, vertOffset);
//				ipspace.camera.setTranslation(isize2 - camOffset, -isize2, -dist);
//				ipspace.camera.lookAt(isize2, isize2, 0f);		
//			} else if (i==1) {
//				dist *= 1.1f;
//				ipspace.camera.setTranslation(isize2, -isize2, -dist);
//				ipspace.camera.lookAt(isize2, isize2, 0f);						
//			} else if (i==2) {
//				pdf.translate(xlateDist, vertOffset);
//				ipspace.camera.setTranslation(isize2 + camOffset, -isize2, -dist);
//				ipspace.camera.lookAt(isize2, isize2, 0f);						
//			}
//			
			ipspace.ignoreDraw = false;
			ipspace.redraw();
			
			
			while (ipspace.inDraw) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ex) {}
			}
			pdf.popMatrix();
		}
		
		ipspace.endRaw();
		
		System.out.println("Done!");
		
		ipspace.exit();
	}
}
