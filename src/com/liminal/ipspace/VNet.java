/*
 * Created on 28-Dec-2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace;

import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import processing.core.PGraphics;

import com.liminal.ipspace.data.Net;
import com.liminal.p5.util.Shape;

public class VNet {

	private Net net;
	private VNet parent;
	private HArea[] areas;
	private SoftReference<VNet[]> subnetRef;
//	private SoftReference<VNet[]> hostRef;
	
	private VNDrawInfo drawInfo;
	
	private Shape shape;
	
	private boolean open = false;
	
	public VNet(VNet parent, Net net) {
		this.parent = parent;
		this.net = net;

		// calculate the HAreas:
		if (parent == null) {
			if (IPSpace.getLayoutFunction() == IPSpace.LayoutFunction.HILBERT)
				areas = new HArea[] { HArea.getRoot() };
			else if (IPSpace.getLayoutFunction() == IPSpace.LayoutFunction.SIERPINSKI_KNOPP)
				areas = new HArea[] { SArea.getRoot() };
			else
				throw new IllegalArgumentException("Unknown layout function!");
		} else {
//System.out.println("Net: " + net.getRange().getMin() + " - " + net.getRange().getMax() + "  (" + net.getRange() + ")");
			ArrayList<HArea> areaList = new ArrayList<HArea>(2);
			for (int i=0; i < parent.areas.length; i++) {
//System.out.println("parent: " + i);
				createAreas(net, parent.areas[i], areaList);
			}
			areas = areaList.toArray(new HArea[areaList.size()]);
		}
		
		createShape();
	}
	
	public Net getNet() { return net; }
	
	public VNet getParent() { return parent; }
	
	
	/** May return null if the subnets aren't known yet */
	public VNet[] getSubnets() {
		VNet[] subnets = null;
		if (subnetRef != null) {
			subnets = subnetRef.get();
		}
//		if (net.isHost()) {
//			subnets = new VNet[0]; 
//		}
		if (subnets == null) {
			// create the sub vnets
			Net[] sns = net.getSubnets();
			if (sns != null) {
				subnets = new VNet[sns.length];
				for (int i = 0; i < sns.length; i++) {
					Net net = sns[i];
					subnets[i] = new VNet(this, net);
				}
				subnetRef = new SoftReference<VNet[]>(subnets);
			}
		}
		return subnets;
	}
	
	public boolean hasSubnetsReady() {
		return (subnetRef != null && subnetRef.get() != null) /* || net.isHost() */ || net.hasSubnetsReady();
	}
	
//	/** creates the hosts locally (they don't get stored anywhere), so they are only soft-referenced and can be garbage collected as necessary */
//	public VNet[] getHosts() {
//		VNet[] hosts = null;
//		if (hostRef != null)
//			hosts = hostRef.get();
//		if (hosts == null) {
//			// create the hosts:
//			long min = net.getRange().min();
//			long max = net.getRange().max();
//			int range = (int) (max - min + 1);
//			hosts = new VNet[range];
//			int count = 0;
//			for (long n=min; n <= max; n++) {
//				Net hostNet = new Net(net, new AddressRange(new Address(n), new Address(n)), net.getName(), net.getId() + "." + count);
//				VNet vhostNet = new VNet(this, hostNet);
//				hosts[count] = vhostNet;
//				count++;
//			}
//			hostRef = new SoftReference<VNet[]>(hosts);
//		}
//		return hosts;	
//	}
	
//	/** May return null if the subnets aren't known yet */
//	public VNet[] getSubnets() {
//		VNet[] subnets = null;
//		if (subnetRef != null) {
//			subnets = subnetRef.get();
//		}
//		return subnets;
//	}
//	
//	public void loadSubnets() {
//		VNet[] subnets = null;
//		if (subnetRef != null) {
//			subnets = subnetRef.get();
//		}
//		if (subnets == null) {
//			// create the sub vnets
//			Net[] sns = net.getSubnets();
//			if (sns != null) {
//				subnets = new VNet[sns.length];
//				for (int i = 0; i < sns.length; i++) {
//					Net net = sns[i];
//					subnets[i] = new VNet(this, net);
//				}
//				subnetRef = new SoftReference<VNet[]>(subnets);
//			}
//		}
//	}
	
	public HArea[] getAreas() {
		return areas;
	}
	
	public boolean isOpen() { return open; }
	public void setOpen(boolean open) {
		this.open = open;
		if (open)
			getSubnets();
	}
	
	public boolean containsPt(float x, float y) {
		for (HArea area : areas) {
			if (area.containsPt(x, y))
				return true;
		}
		return false;
	}
	
	public Area getAwtArea() {
if (areas.length == 0)
	return null; // shouldn't happen! only here as a line to put a breakpoint on!
		Area a = areas[0].getAwtArea();
		for (int i=1; i < areas.length; i++)
			a.add(areas[i].getAwtArea());
		return a;
	}
	
	private void createShape() {
		Area a = getAwtArea();
		PathIterator pi = a.getPathIterator(null);
		
		float[] coords = new float[6];
		ArrayList<float[]> pts = new ArrayList<float[]>();
		while (!pi.isDone()) {
			int type = pi.currentSegment(coords);
			switch (type) {
			case PathIterator.SEG_MOVETO:
			case PathIterator.SEG_LINETO:
				pts.add(new float[] {coords[0], coords[1]});
				break;
			case PathIterator.SEG_CLOSE:
				break;
			default:
				throw new IllegalStateException("Unexpected type of path move: " + type);
			}
			pi.next();
		}
//System.out.println("Net: " + net.getRange().getMin() + " - " + net.getRange().getMax() + "  (" + net.getRange() + ")");
		int numPts = pts.size();
		
		// check if the first and last are identical, and if so, ignore the last:
		float[] pt0 = pts.get(0);
		float[] ptn = pts.get(numPts-1);
		if (pt0[0] == ptn[0] && pt0[1] == ptn[1])
			numPts--;
		
		shape = new Shape(PGraphics.POLYGON, numPts);
		for (int i=0; i < numPts; i++) {
			float[] pt = pts.get(i);
//float x = pt[0];
//float y = pt[1];
//System.out.println("\t" + x + ", " + y);
			shape.setVertex(i, pt[0], pt[1], 0f);
		}
	}
	
	private static void createAreas(Net net, HArea parent, List<HArea> areas) {
		HArea[] children = parent.getChildAreas();
		for (int i=0; i < children.length; i++) {
			HArea carea = children[i];
			long cmin = carea.getMinAddress();
			long cmax = carea.getMaxAddress();
//System.out.println("\tarea " + i + ": " + new com.liminal.ipspace.data.Address(cmin) + " - " + new com.liminal.ipspace.data.Address(cmax));
			if (net.getRange().intersects(cmin, cmax)) {
//System.out.println("\t\tintersects");
				if (net.getRange().contains(cmin, cmax)) {
//System.out.println("\t\tcontains");
					areas.add(carea);
				} else {
//System.out.println("\t\tsubareas");
					createAreas(net, carea, areas);
				}
			}
		}
	}
	
	public Shape getShape() { return shape; }
	
//	public void draw(PGraphics3D g) {
//		g.stroke(g.color(0, 0, 255));
//		if (!open)
//			g.fill(g.color(0, 0, 255, 25));
//		else
//			g.noFill();
//		shape.draw(g, true, !open);
//		if (open) {
//			VNet[] subnets = getSubnets();
//			if (subnets != null) {
//				for (int i=0; i < subnets.length; i++) {
//					subnets[i].draw(g);
//				}
//			}
//		} else {
//		}
//	}
	
	public VNDrawInfo getDrawInfo() { return drawInfo; }
	public void setDrawInfo(VNDrawInfo dinfo) { drawInfo = dinfo; }
	
	
}
