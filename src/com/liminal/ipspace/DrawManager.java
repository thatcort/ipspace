/*
 * Created on 9-Feb-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;

import javax.media.opengl.GL;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastTable;
import processing.core.PConstants;
import processing.core.PFont;
import processing.core.PGraphics3D;
import processing.opengl.PGraphicsOpenGL;

import com.liminal.ipspace.data.AddressRange;
import com.liminal.ipspace.data.Net;
import com.liminal.p5.util.Frustum;
import com.liminal.p5.util.QTNode;

public class DrawManager {

	private IPSpace ipspace;
	
	private Frustum frustum;
	
	private QTNode quadTree;
	
	private PFont font;
	
	private float[] pick;
	private VNet pickNet = null;
	private VNet prevPickNet = null; // last frame's picked net
	private String netText = null;
	private boolean drawText = true;
	
	private boolean loadingBlink = true; // alternates between true and false in time with the blinking
	private long blinkDuration = 250; // millis
	private long lastBlink = -1L;
	
	private FastMap<QTNode, Float> nodeDistanceMap = new FastMap<QTNode, Float>();
	private FastMap<QTNode, Float> nodeSizeProportionMap = new FastMap<QTNode, Float>();
	
	private FastList<VNet> netsToOpen = new FastList<VNet>(); // closed nets without ready subnets that we think should be open
	private FastList<VNet> netsOpening = new FastList<VNet>(); // nets we've asked to open that we're waiting for
	
	private boolean drawNumberLine = (IPSpace.getMode() == IPSpace.Mode.CURRENT);
	private VNet previousDrawVNet;
	
	private int maxNetsToOpenAtOnce = -1; // max to open at any given time, zero or less means no limit
	
	private int minClosedNetLevel = 0; // minimum level of net that can be closed 
	
	private boolean drawNets = true;
	private float lineLevelZFactor = 0f;

	private boolean drawPoints = false;
	private boolean colorPoints = true;
	
	private long pickEnterTime = 0; // time we entered the currently picked net
	private boolean loadingNetInfo = false;
	
	private int tooltipDelay = 750; // milliseconds
	
	public DrawManager(IPSpace ipspace) throws IOException {
		this.ipspace = ipspace;
		
		QTNode.setMinSize(64f); // limits the total # of nodes to a reasonable number (for 64 about 140,000, for 32 about 560,000)
		
//		String fontString = "data/LucidaSansUnicode-11.vlw";
		String fontString = "data/BitstreamVeraSansMono-Roman-11.vlw";
		InputStream fontInput = new FileInputStream(new File(fontString));
		font = new PFont(fontInput);
		
		VNet vinternet = ipspace.getVInternet();
		Rectangle rect = vinternet.getAwtArea().getBounds();
		quadTree = new QTNode(null, rect.x, rect.y, Math.max(rect.width, rect.height));
		VNDrawInfo info = new VNDrawInfo(quadTree);
		vinternet.setDrawInfo(info);
		
	}
	
	public void draw(PGraphics3D g) {
		
		pickNet = null;
		
		long t = System.currentTimeMillis();
		if (t - lastBlink > blinkDuration) {
			loadingBlink = !loadingBlink;
			lastBlink = t;
		}
		
		frustum = new Frustum(g);
		
		pick = ipspace.getPickState().pick(2, 0f);
		
		if (ipspace.cameraMoved()) {
			nodeDistanceMap.clear();
			nodeSizeProportionMap.clear();
		}
		
		previousDrawVNet = null;
		
		drawNet(ipspace.getVInternet(), g, true);
		
		if (drawText) {
			if (pickNet != null) { //  && !pickNet.getNet().isHost()) { // don't pick hosts yet
				g.fill(g.color(255f));
				if (ipspace.isUsingOpenGL()) {
					 ((PGraphicsOpenGL)g).gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
					 g.camera();
					 g.perspective(); 
				} else {
					g.textMode(PConstants.SCREEN);
				}
				g.textFont(font);
				
				if (pickNet != prevPickNet) {
					if (loadingNetInfo)
						ipspace.getDataManager().cancelNetInfoRequest(prevPickNet.getNet());
					pickEnterTime = t;
					netText = null;
				}
				
				boolean showFullInfo = netText != null || (!ipspace.cameraMoved() && t - pickEnterTime > tooltipDelay);
//				boolean showFullInfo = netText != null || (t - pickEnterTime > tooltipDelay);
				if (showFullInfo) {
					if (prevPickNet != pickNet || netText == null) {
	//					System.out.println("Pick: " + vnet.getNet());
						netText = pickNet.getNet().getWhoisInfo();
					}
					
					if (netText != null) {
						g.text(netText, 20, 20);
						loadingNetInfo = false;
					} else {
						g.text(pickNet.getNet().getRange().toString() + "\nLoading details...", 20, 20);
						loadingNetInfo = true;
					}
				} else {
					Net net = pickNet.getNet();
					String str = net.getRange().toString() + "\n" + net.getRegistry();
					g.text(str, 20, 20);
				}
			} else {
				if (prevPickNet != null && loadingNetInfo) {
					ipspace.getDataManager().cancelNetInfoRequest(prevPickNet.getNet());
					loadingNetInfo = false;
				}
			}
			

		}
		
		openNets();
		
		prevPickNet = pickNet;

	}
	
	
	private void drawNet(VNet vnet, PGraphics3D g, boolean testPick) {
	
		Net net = vnet.getNet();
		
		VNDrawInfo info = vnet.getDrawInfo();
		if (info == null) {
			info = createDrawInfo(vnet);
			vnet.setDrawInfo(info);
		}
		
		QTNode qtnode = info.qtNode;
		
		// make sure the net is at least partially visible:
		if (/*vnet.getNet().getLevel() >= 1 && */frustum.contains(qtnode) == Frustum.Containment.OUTSIDE) {
//			System.out.println("frustum rejected: " + vnet.getNet());
			previousDrawVNet = vnet;
			return;
		}

		boolean drawChildren = shouldDrawChildren(vnet);
		
		ColorScheme colors = ipspace.getColorScheme();
		int outlineColor = colors.getOutlineColor(vnet);
		int fillColor = colors.getFillColor(vnet);
		
		VNet[] subnets = null;
		boolean hasSubnetsReady = vnet.hasSubnetsReady();
		if (drawChildren) {
			if (hasSubnetsReady) {
				subnets = vnet.getSubnets();
//				netsOpening.remove(vnet);
				int index = Collections.binarySearch(netsOpening, vnet, vnetDistanceComparator);
				if (index >= 0)
					netsOpening.remove(index); // remove from the opening list
			} else {
				netsToOpen.add(vnet);
			}
		} else {
			vnet.setOpen(false);
		}
		
		if (info.loading)
			info.loading = !hasSubnetsReady;
		
		boolean notDrawingSubnets = (!drawChildren || !hasSubnetsReady || subnets.length == 0);
		

		// find the pickNet
//		boolean containsPick = testPick && qtnode.contains(pick[0], pick[1]) && vnet.containsPt(pick[0], pick[1]);
		boolean containsPick = testPick && !ipspace.cameraMoved() && qtnode.contains(pick[0], pick[1]) && vnet.containsPt(pick[0], pick[1]);
		if (containsPick && notDrawingSubnets) { //  && !vnet.getNet().isHost()) { // don't pick hosts for now...
			pickNet = vnet;
//			if (prevPickNet != vnet || netText == null) {
////				System.out.println("Pick: " + vnet.getNet());
//				if (drawText)
//					netText = net.getWhoisInfo();
//			}
		}
		
		if (info.loading) {
			if (loadingBlink) {
				g.noFill();
				g.noStroke();
			} else {
				g.stroke(outlineColor);
				g.fill(fillColor);
			}
		} else {
			g.stroke(outlineColor);
			
			if (!drawChildren || (hasSubnetsReady && subnets.length == 0))
				g.fill(fillColor);
			else
				g.noFill();
		}
		
		if (drawNets && net.isAllocated())
			vnet.getShape().draw(g); // , !drawChildren || (info.loading && loadingBlink), !drawChildren);

		if (subnets != null) {
			if (subnets.length > 0) {
				for (int i=0; i < subnets.length; i++) {
					drawNet(subnets[i], g, containsPick);
				}
			} else {
//				AddressRange range = vnet.getNet().getRange();
//				if (net.isAllocated() && containsPick && (range.max() - range.min() < 1024)) {
//					VNet[] hosts = vnet.getHosts();
//					for (VNet host : hosts) {
//						drawNet(host, g, containsPick);
//					}
//				}
				if (net.isAllocated() && shouldDrawHosts(vnet)) {
					drawHosts(vnet, g);
				}
			}
		}
		
		// draw the number line between closed nets
		if ((drawNumberLine || drawPoints) && notDrawingSubnets) {
			drawNumberLine(vnet, g);
			previousDrawVNet = vnet;
		}

		// if net is picked and there are no subnets (either closed or loading), then draw it highlighted:
		if (containsPick && notDrawingSubnets) {
			g.stroke(colors.getPickOutlineColor(vnet));
			g.fill(colors.getPickFillColor(vnet));
			vnet.getShape().draw(g); // , true, true);
		}
		
	}
	
	private void openNets() {
		
		Collections.sort(netsToOpen, vnetDistanceComparator);
		int numOpening = 0;
		for (VNet vnet : netsToOpen) {
			if (maxNetsToOpenAtOnce >= 0 && numOpening >= maxNetsToOpenAtOnce)
				break;

			numOpening++;
//			if (!vnet.getDrawInfo().loading) {
				vnet.getSubnets();
				vnet.getDrawInfo().loading = true;
//					numOpening++;
//			}
			int index = Collections.binarySearch(netsOpening, vnet, vnetDistanceComparator);
			if (index >= 0)
				netsOpening.remove(index); // remove from the opening list
		}
		for (VNet  vnet : netsOpening) {
			// cancel the subnet request
			boolean cancelled = ipspace.getDataManager().cancelSubnetRequest(vnet.getNet());
			System.out.println("\tCacellation " + (cancelled ? "successful" : "failed"));
			if (cancelled)
				vnet.getDrawInfo().loading = false;
		}
		FastList<VNet> tmp = netsOpening;
		netsOpening = netsToOpen;
		netsToOpen = tmp;
		netsToOpen.clear();
	}
//	private void openNets() {
//		Collections.sort(netsToOpen, vnetDistanceComparator);
//		int numOpening = 0;
//		for (VNet vnet : netsToOpen) {
//			if (vnet.getDrawInfo().loading) {
//				numOpening++;
//			} else {
//				vnet.getSubnets();
//				vnet.getDrawInfo().loading = true;
//			}
//			if (maxNetsToOpenAtOnce > 0 && numOpening >= maxNetsToOpenAtOnce)
//				break;
//		}
//	}
	
	private Comparator<VNet> vnetDistanceComparator = new Comparator<VNet>() {
		public int compare(VNet n1, VNet n2) {
			float d1 = getDistance(n1.getDrawInfo().qtNode);
			float d2 = getDistance(n2.getDrawInfo().qtNode);
			return Float.compare(d1, d2);
		}
		
	};
	
	// draw the number line
	private void drawNumberLine(VNet vnet, PGraphics3D g) {
		ColorScheme colors = ipspace.getColorScheme();
		int lineColor = colors.getNumberLineColor();
		int pointColor = colors.getPointColor(vnet);
		
		int i=0;
		HArea[] nextAreas = vnet.getAreas();
//		Rectangle2D.Float rect = null;
		
		float px, py, pz;
		
		// find the previous line position:
		if (previousDrawVNet != null) {
			VNet prevNet = getLastClosedVNet(previousDrawVNet);
			g.stroke(colorPoints ? colors.getPointColor(previousDrawVNet) : lineColor);
			HArea[] prevAreas = prevNet.getAreas();
//			rect = prevAreas[prevAreas.length-1].getBoundsRect();
			HArea pa = prevAreas[prevAreas.length-1];
			px = pa.getCenterX();
			py = pa.getCenterY();
		} else { // previous net is null, so must be the first, so we start from this net
			HArea ha = nextAreas[0];
			px = ha.getCenterX();
			py = ha.getCenterY();
//			rect = nextAreas[0].getBoundsRect();
			i = 1; // skip the first area in the loop
			g.stroke(colorPoints ? pointColor : lineColor);
		}
		
		boolean dp = vnet.getNet().isAllocated() && drawPoints;
//		float px = rect.x + rect.width*.5f;
//		float py = rect.y + rect.height*.5f;
		pz = (previousDrawVNet != null ? previousDrawVNet.getNet().getLevel() : vnet.getNet().getLevel()) * lineLevelZFactor;
		for (; i < nextAreas.length; i++) {
//			rect = nextAreas[i].getBoundsRect();
//			float nx = rect.x + rect.width*.5f;
//			float ny = rect.y + rect.height*.5f;
//			float nz = vnet.getNet().getLevel() * lineLevelZFactor;
			HArea ha = nextAreas[i];
			float nx = ha.getCenterX();
			float ny = ha.getCenterY();
			float nz = vnet.getNet().getLevel() * lineLevelZFactor;
			if (drawNumberLine) {
//				g.stroke(lineColor);
//				g.line(px, py, pz, nx, ny, nz);
				g.beginShape(PConstants.LINES);
				g.vertex(px, py, pz);
				g.stroke(colorPoints ? pointColor : lineColor);
				g.vertex(nx, ny, nz);
				g.endShape(PConstants.OPEN);
			}
			if (dp) {
				g.stroke(colorPoints ? pointColor : lineColor);
				g.point(nx, ny, nz);
			}
			px = nx;
			py = ny;
			pz = nz;
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
	
	private void drawHosts(VNet vnet, PGraphics3D g) {
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
	
	/** used to draw the picked net over other nets after other have been drawn */
	private void drawPickNet() {
		if (prevPickNet != null) {
			
		}
	}

	private VNDrawInfo createDrawInfo(VNet vnet) {
		QTNode qt = findQTNode(vnet);
		VNDrawInfo info = new VNDrawInfo(qt);
		return info;
	}
	
	private QTNode findQTNode(VNet vnet) {
		VNet pnet = vnet.getParent();
		VNDrawInfo pinfo = pnet.getDrawInfo(); 
		QTNode pqt = pinfo.qtNode;
		QTNode qt = pqt.getSmallestContainingNode(vnet.getAwtArea().getBounds2D());
		return qt;
	}
	
	private float getDistance(QTNode qt) {
		Float distF = nodeDistanceMap.get(qt);
		if (distF == null) {
			float x = qt.getCenterX();
			float y = qt.getCenterY();
			float dist = (float) Math.sqrt(ipspace.getPickState().getDistance2(x, y, 0f));
			distF = new Float(dist);
			nodeDistanceMap.put(qt, distF);
		}
		return distF;
	}
	
	public boolean shouldDrawChildren(VNet vnet) {
		if (vnet.getNet().getLevel() < minClosedNetLevel)
			return true;
		QTNode qt = vnet.getDrawInfo().qtNode;
		boolean shouldDraw = shouldDrawChildren(qt);
		return shouldDraw;

		
		
//		return vnet.getNet().getLevel() < 1;
		
		
//		&& 
//			(vnet.getNet().getRegistry().equals(com.liminal.ipspace.whois.WhoisManager.RIPE)
//					|| vnet.getNet().getRegistry().equals(com.liminal.ipspace.whois.WhoisManager.IANA));
		
//		QTNode qt = vnet.getDrawInfo().qtNode;
//		
//		float dist = Float.POSITIVE_INFINITY;
//		Float distF = nodeDistanceMap.get(qt);
//		if (distF == null) {
//			dist = getDistance(qt);
//			nodeDistanceMap.put(qt, dist);
//		} else {
//			dist = distF.floatValue();
//		}
//		
//		return qt.getSize() / dist > .5f;
	}
	
	private float minProportion = 0.125f;
	
	private boolean shouldDrawChildren(QTNode qt) {
		Float propF = nodeSizeProportionMap.get(qt);
		if (propF == null) {
			if (qt.getParent() != null) {
				if (!shouldDrawChildren(qt.getParent()))
					return false;
			}
			float qtDist = getDistance(qt);
			float qtScreenSize = 2 * qtDist * (float) Math.tan(ipspace.getCamera().getFOV() * .5);
			float proportion = qt.getSize() / qtScreenSize;
			propF = new Float(proportion);
			nodeSizeProportionMap.put(qt, propF);
		}
		return propF.floatValue() >= minProportion;
	}
	
	private boolean shouldDrawHosts(VNet vnet) {
		QTNode qt = vnet.getDrawInfo().qtNode;
		float qtDist = getDistance(qt);
		float qtScreenSize = 2 * qtDist * (float) Math.tan(ipspace.getCamera().getFOV() * .5);
		float proportion = 1f / qtScreenSize; // hosts are always a unit sized
//System.out.println("host proportion: " + proportion);
		return proportion >= 0.004;
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
	public void setLineLevelZFactor(float f) {
		lineLevelZFactor = f;
	}
	public float getLineLevelZFactor() {
		return lineLevelZFactor;
	}
	public void setMinClosedNetLevel(int mcnl) {
		minClosedNetLevel = mcnl;
	}
	public boolean getDrawText() {
		return drawText;
	}
	public void setDrawText(boolean dt) {
		drawText = dt;
	}
	
}
