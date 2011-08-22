/*
 * File:      ImageGenerator.java
 * Created:   30-Oct-06
 * Author:    bcort
 *
 * Copyright (c) 2003-2006 by Oculus Info Inc.  All rights reserved.
 *
 * $Id$
 *
 */
package com.liminal.ipspace;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.liminal.ipspace.IPSpace.LayoutFunction;
import com.liminal.ipspace.data.AddressRange;
import com.liminal.ipspace.data.DataManager;
import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.whois.WhoisManager;

public class ImageGenerator2D {
	private static Logger LOG;
	
	private static AddressRange renderRange = null;
	private static long minGetSubnetRange = 257;

	private static String imageFormat = "tif";
	private static float dpi = 300f;
	
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

		private DrawManager2D2 drawMgr;
		
		private float marginPct = .05f;
		
		private float netStrokeWeight;
		private float lineStrokeWeight;
		
		public NoLoopIPSpace(DataManager dataMgrParam) throws Throwable {
			super(dataMgrParam);
			drawMgr = new DrawManager2D2(this);
			drawMgr.setDrawRange(renderRange);
			drawMgr.setMinClosedNetLevel(getMinNetLevel());
			drawMgr.setMinGetSubnetRange(minGetSubnetRange);
			
			String marginStr = IPSpace.getCommandLineArgValue("marginpct");
			if (marginStr != null) {
				try {
					marginPct = Float.valueOf(marginStr);
				} catch (NumberFormatException nfe) {
					LOG.warning("Unable to parse float value for percent margin: " + marginStr + ". Using default value " + marginPct);
				}				
			}
			
			String drawLineStr = IPSpace.getCommandLineArgValue("drawline");
			if (drawLineStr != null && !drawLineStr.toLowerCase().startsWith("t") && !drawLineStr.toLowerCase().startsWith("y")) {
				drawMgr.setDrawNumberLine(false);
				System.out.println("Not drawing number line.");
			}
			String drawNetStr = IPSpace.getCommandLineArgValue("drawnets");
			if (drawLineStr != null && !drawNetStr.toLowerCase().startsWith("t") && !drawNetStr.toLowerCase().startsWith("y")) {
				drawMgr.setDrawNets(false);
				System.out.println("Not drawing net areas line.");
			}
			
			netStrokeWeight = .01f;
			String swStr = IPSpace.getCommandLineArgValue("netstrokeweight");
			if (swStr != null) {
				try {
					netStrokeWeight = Float.valueOf(swStr);
				} catch (NumberFormatException nfe) {
					LOG.warning("Unable to parse float value for net stroke weight: " + swStr + ". Using default value " + netStrokeWeight);
				}
			}
			// convert from points to pixels:
			float netStrokePx = netStrokeWeight * dpi / 72;
			LOG.info("Net stroke weight: " + netStrokeWeight + " pts. --> " + netStrokePx + " px.");
			drawMgr.setNetStrokeWeight(netStrokePx);
			
			lineStrokeWeight = .01f;
			swStr = IPSpace.getCommandLineArgValue("linestrokeweight");
			if (swStr != null) {
				try {
					lineStrokeWeight = Float.valueOf(swStr);
				} catch (NumberFormatException nfe) {
					LOG.warning("Unable to parse float value for line stroke weight: " + swStr + ". Using default value " + netStrokeWeight);
				}
			}
			// convert from points to pixels:
			float lineStrokePx = lineStrokeWeight * dpi / 72;
			LOG.info("Line stroke weight: " + lineStrokeWeight + " pts --> " + lineStrokePx + " px.");
			drawMgr.setLineStrokeWeight(lineStrokePx);
		}

		@Override
		public void setup() {
			size(sizeX, sizeY, JAVA2D);
			g.strokeJoin(MITER);
			g.fill(0);

//			colorScheme = new PDFColorScheme(this);
			colorScheme = new BlackColorScheme();
			
			background(colorScheme.getBackgroundColor());
			
			noLoop();
		}
		
		public volatile boolean ignoreDraw = true;
		
		@Override
		public void draw() {
			if (ignoreDraw)
				return;
			
			float scale = (1 - 2 * marginPct) * sizeX / HArea.getRoot().getLineDist();
			float xlate = marginPct * sizeX;
			
			translate(xlate, sizeX - xlate);
			scale(scale, -scale);
			
			drawMgr.setNetStrokeWeight(netStrokeWeight / scale);
			drawMgr.setLineStrokeWeight(lineStrokeWeight / scale);
			
			background(colorScheme.getBackgroundColor());

			LOG.info("Drawing...");
			drawMgr.draw(g);
			LOG.info("Done drawing.");
			
			String fileName = "ipspace_" + (getLayoutFnArg() == LayoutFunction.HILBERT ? 'H' : 'S') + getMinNetLevel() + "_" + drawMgr.getNetStrokeWeight() + "_" + drawMgr.getLineStrokeWeight() + "_" + (renderRange != null ? renderRange.toString() +"_" : "") + startTime + "." + imageFormat;
			System.out.println("Saving to file: " + fileName);

			save(fileName);
			
			System.out.println("Done.");
			exit();
			
		}
	}
	
	private static LayoutFunction getLayoutFnArg() {
		String fv = IPSpace.getCommandLineArgValue("layout");
		if (fv != null)
			return fv.toLowerCase().startsWith("h") ? LayoutFunction.HILBERT : LayoutFunction.SIERPINSKI_KNOPP;
		return LayoutFunction.HILBERT;
	}
	
	public static void main(String[] args) throws Throwable {
		IPSpace.setMode(IPSpace.Mode.CURRENT);
		IPSpace.VALID_SOURCES = WhoisManager.BOTH_SOURCES;
		IPSpace.parseCommandLineArgs(args);
		
		String rangeStr = IPSpace.getCommandLineArgValue("range");
		if (rangeStr != null) {
			renderRange = AddressRange.parseAddressRange(rangeStr);
		}
	
		String mgsrStr = IPSpace.getCommandLineArgValue("mingetsubnet");
		if (mgsrStr != null) {
			minGetSubnetRange = Long.parseLong(mgsrStr);
		}
		
		String formatStr = IPSpace.getCommandLineArgValue("format");
		if (formatStr != null) {
			imageFormat = formatStr.toLowerCase();
		}

		String dpiStr = IPSpace.getCommandLineArgValue("dpi");
		if (dpiStr != null) {
			dpi = Float.parseFloat(dpiStr);
		}
				
		
		LOG = Logger.getLogger(ImageGenerator2D.class.getName());
		
		DataManager dm = new SynchronousDataManager(false);
		
		NoLoopIPSpace ipspace = new NoLoopIPSpace(dm);
		
		while (!dm.isFinishedProcessingRirDelegations()) {
			try {
				System.out.println("Waiting for rir delegation processing... tick.");
				dm.tick();
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
		ipspace.setLayoutFunction(getLayoutFnArg());
		ipspace.ignoreDraw = false;
		ipspace.redraw();
	}


}
