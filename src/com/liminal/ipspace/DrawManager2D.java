/*
 * File:      DrawManager2D.java
 * Created:   18-Oct-06
 * Author:    bcort
 *
 * Copyright (c) 2003-2006 by Oculus Info Inc.  All rights reserved.
 *
 * $Id$
 *
 */
package com.liminal.ipspace;

import java.awt.geom.Rectangle2D;
import java.io.IOException;

import javolution.util.FastList;
import processing.core.PConstants;
import processing.core.PGraphics;

import com.liminal.ipspace.data.AddressRange;
import com.liminal.ipspace.data.Net;

public class DrawManager2D {
	private IPSpace ipspace;
	
	private boolean drawNumberLine = (IPSpace.getMode() == IPSpace.Mode.CURRENT);
	private VNet previousDrawVNet;
	
	private int minClosedNetLevel = 0; // minimum level of net that can be closed
	
	private FastList<VNet> netsToOpen = new FastList<VNet>(); // closed nets without ready subnets that we think should be open
	
	private boolean drawNets = true;

	private boolean drawPoints = false;
	private boolean colorPoints = true;
	
	private AddressRange drawRange = null;
	
	public DrawManager2D(IPSpace ipspace) throws IOException {
		this.ipspace = ipspace;
	}
	
	public void draw(PGraphics g) {
		
		previousDrawVNet = null;
		
		drawNet(ipspace.getVInternet(), g);
		
	}
	
	
	private void drawNet(VNet vnet, PGraphics g) {
	
		Net net = vnet.getNet();
		
		// make sure we should draw the net:
		if (drawRange != null && !drawRange.intersects(net.getRange())) {
			previousDrawVNet = vnet;
			return;			
		}

		boolean drawChildren = shouldDrawChildren(vnet);
		vnet.setOpen(drawChildren);
		
		ColorScheme colors = ipspace.getColorScheme();
		int outlineColor = colors.getOutlineColor(vnet);
		int fillColor = colors.getFillColor(vnet);
		
		VNet[] subnets = null;
		boolean hasSubnetsReady = vnet.hasSubnetsReady();
		if (drawChildren) {
			if (hasSubnetsReady)
				subnets = vnet.getSubnets();
			else
				netsToOpen.add(vnet);
		}
		
		boolean notDrawingSubnets = (!drawChildren || !hasSubnetsReady || subnets.length == 0);
		
		g.stroke(outlineColor);
		
		if (!drawChildren || (hasSubnetsReady && subnets.length == 0))
			g.fill(fillColor);
		else
			g.noFill();
		
		if (drawNets && net.isAllocated())
			vnet.getShape().draw2D(g); // , !drawChildren, !drawChildren);

		if (subnets != null) {
//			g.pushMatrix();
//			g.translate(0f, 0f, -100f);
			if (subnets.length > 0) {
				for (int i=0; i < subnets.length; i++) {
					drawNet(subnets[i], g);
				}
			} else {
				if (net.isAllocated() && shouldDrawHosts(vnet)) {
					drawHosts(vnet, g);
				}
			}
//			g.popMatrix();
		}
		
		// draw the number line between closed nets
		if ((drawNumberLine || drawPoints) && notDrawingSubnets) {
			drawNumberLine(vnet, g);
			previousDrawVNet = vnet;
		}

	}
	
	
	// draw the number line
	private void drawNumberLine(VNet vnet, PGraphics g) {
		ColorScheme colors = ipspace.getColorScheme();
		int lineColor = colors.getNumberLineColor();
		int pointColor = colors.getPointColor(vnet);
		
		int i=0;
		HArea[] nextAreas = vnet.getAreas();
		
		float px, py;
		
		// find the previous line position:
		if (previousDrawVNet != null) {
			VNet prevNet = getLastClosedVNet(previousDrawVNet);
			g.stroke(colorPoints ? colors.getPointColor(previousDrawVNet) : lineColor);
			HArea[] prevAreas = prevNet.getAreas();
			HArea pa = prevAreas[prevAreas.length-1];
			px = pa.getCenterX();
			py = pa.getCenterY();
		} else { // previous net is null, so must be the first, so we start from this net
			HArea ha = nextAreas[0];
			px = ha.getCenterX();
			py = ha.getCenterY();
			i = 1; // skip the first area in the loop
			g.stroke(colorPoints ? pointColor : lineColor);
		}
		
		boolean dp = vnet.getNet().isAllocated() && drawPoints;
		for (; i < nextAreas.length; i++) {
			HArea ha = nextAreas[i];
			float nx = ha.getCenterX();
			float ny = ha.getCenterY();
			if (drawNumberLine) {
				g.beginShape(PConstants.LINES);
				g.vertex(px, py);
				g.stroke(colorPoints ? pointColor : lineColor);
				g.vertex(nx, ny);
				g.endShape(PConstants.OPEN);
			}
			if (dp) {
				g.stroke(colorPoints ? pointColor : lineColor);
				g.point(nx, ny);
			}
			px = nx;
			py = ny;
		}
	}
	
	private VNet getLastClosedVNet(VNet vnet) {
		if (vnet.isOpen()) {
			VNet[] subnets = vnet.getSubnets();
			if (subnets != null && subnets.length > 0)
				return getLastClosedVNet(subnets[subnets.length-1]);
		}
		return vnet;
	}
	
	private void drawHosts(VNet vnet, PGraphics g) {
		g.stroke(ipspace.getColorScheme().getHostColor());
		HArea[] areas = vnet.getAreas();
		for (HArea ha : areas) {
			Rectangle2D.Float rect = ha.getBoundsRect();
			final float maxX = rect.x + rect.width;
			final float maxY = rect.y + rect.height;
			for (float x = rect.x + .5f; x < maxX; x += 1f) {
				for (float y = rect.y + .5f; y < maxY; y += 1f) {
					g.point(x, y);
				}
			}
		}
	}
	
	
	public boolean shouldDrawChildren(VNet vnet) {
		return  (vnet.getNet().getLevel() < minClosedNetLevel);
	}
	
	
	private boolean shouldDrawHosts(VNet vnet) {
		return false;
	}
	
	public void setDrawNumberLine(boolean dnl) {
		drawNumberLine = dnl;
	}
	public boolean getDrawNumberLine() {
		return drawNumberLine;
	}
	public void setDrawPoints(boolean dp) {
		drawPoints = dp;
	}
	public boolean getDrawPoints() {
		return drawPoints;
	}
	public boolean getColorPoints() {
		return colorPoints;
	}
	public void setColorPoints(boolean cp) {
		colorPoints = cp;
	}
	public void setDrawNets(boolean dn) {
		drawNets = dn;
	}
	public boolean getDrawNets() {
		return drawNets;
	}
	public void setMinClosedNetLevel(int mcnl) {
		minClosedNetLevel = mcnl;
	}

	public AddressRange getDrawRange() {
		return drawRange;
	}

	public void setDrawRange(AddressRange drawRange) {
		this.drawRange = drawRange;
	}
}
