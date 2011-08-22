/*
 * File:      SArea.java
 * Created:   29-Mar-2006
 * Author:    bcort
 *
 * Copyright (c) 2003-2004 by Oculus Info Inc.  All rights reserved.
 *
 * $Id$
 *
 */
package com.liminal.ipspace;

import static com.liminal.ipspace.DiagonalTurtle.NE;
import static com.liminal.ipspace.DiagonalTurtle.NW;
import static com.liminal.ipspace.DiagonalTurtle.SE;
import static com.liminal.ipspace.DiagonalTurtle.SW;

import java.awt.Polygon;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.lang.ref.SoftReference;

public class SArea extends HArea {

//	private static final float ROOT_2 = (float) Math.sqrt(2);
	
//	protected Polygon poly = null;
	
	public static HArea getRoot() {
		return new SAreaRoot();
	}
	
	
	public SArea(HArea parent, char symbol, float x, float y,
			int orientation, long linePos) {
		super(parent, symbol, x, y, orientation, linePos);
	}

	
	public HArea[] getChildAreas() {
		HArea[] children = null;
		if (childrenRef != null)
			children = childrenRef.get();
		if (children == null) { // create
			children = new HArea[4];
			DiagonalTurtle dt = new DiagonalTurtle(x, y, orientation);
			long childPos = linePos;
			int childDist = getDist(level+1); // along axis
			int rangeDist = childDist * childDist / 2;
			children[0] = new SArea(this, L, dt.getX(), dt.getY(), dt.getOrientation(), childPos);
			dt.moveManhattan(childDist);
			childPos += rangeDist;
			dt.turnLeft();
			children[1] = new SArea(this, L, dt.getX(), dt.getY(), dt.getOrientation(), childPos);
			dt.moveManhattan(childDist);
			childPos += rangeDist;
			dt.turnRight();
			dt.turnRight();
			children[2] = new SArea(this, L, dt.getX(), dt.getY(), dt.getOrientation(), childPos);
			dt.moveManhattan(childDist);
			childPos += rangeDist;
			dt.turnLeft();
			children[3] = new SArea(this, L, dt.getX(), dt.getY(), dt.getOrientation(), childPos);

//System.out.println("parent: " + new Address(getMinAddress()) + " - " + new Address(getMaxAddress()));			
//System.out.println("\t child: " + new Address(children[0].getMinAddress()) + " - " + new Address(children[0].getMaxAddress()));			
//System.out.println("\t child: " + new Address(children[1].getMinAddress()) + " - " + new Address(children[1].getMaxAddress()));			
//System.out.println("\t child: " + new Address(children[2].getMinAddress()) + " - " + new Address(children[2].getMaxAddress()));			
//System.out.println("\t child: " + new Address(children[3].getMinAddress()) + " - " + new Address(children[3].getMaxAddress()));			

			childrenRef = new SoftReference<HArea[]>(children);
		}
		return children;
	}

	
	public long getMaxAddress() {
		long ld = getLineDist();
		return linePos + (ld * ld)/2 - 1;
	}

	
	public int getLineDist() {
		return getDist(level);
	}

	
	protected static int getDist(int level) {
		if (level == 0) return HArea.getDist(level);
		return HArea.getDist(level) << 1;
	}
	

	public Area getAwtArea() {
		return new Area(getPolygon());
	}

	public Rectangle2D.Float getBoundsRect() {
		if (rect == null) {
			float minX, minY;
			int dist = getLineDist();
			if (orientation == NE || orientation == SE)
				minX = x;
			else
				minX = x - dist;
			if (orientation == NE || orientation == NW)
				minY = y;
			else
				minY = y - dist;
			rect = new Rectangle2D.Float(minX, minY, dist, dist);
		}
		return rect;
	}
	
	public boolean containsPt(float x, float y) {
		return getPolygon().contains(x, y);
	}
	
	protected Polygon getPolygon() {
Polygon poly = null;
		if (poly == null) {
			poly = new Polygon();
			int px = (int) this.x;
			int py = (int) this.y;
			poly.addPoint(px, py);
			int dist = getLineDist();
			if (orientation == NE) {
				py += dist;
				poly.addPoint(px, py);
				px += dist;
				poly.addPoint(px, py);			
			} else if (orientation == SE) {
				px += dist;
				poly.addPoint(px, py);
				py -= dist;
				poly.addPoint(px, py);			
			} else if (orientation == NW) {
				px -= dist;
				poly.addPoint(px, py);
				py += dist;
				poly.addPoint(px, py);			
			} else if (orientation == SW) {
				py -= dist;
				poly.addPoint(px, py);
				px -= dist;
				poly.addPoint(px, py);			
			}
		}
		return poly;
	}
	
	public float getCenterX() {
		float d = getLineDist();
		float c = 0f;
		if (orientation == NE) {
			c =  x + d * .25f;
		} else if (orientation == SE) {
			c =  x + d * .75f;
		} else if (orientation == NW) {
			c =  x - d * .75f;
		} else if (orientation == SW) {
			c =  x - d * .25f;
		}
		return c;
	}
	public float getCenterY() {
		float d = getLineDist();
		float c = 0f;
		if (orientation == NE) {
			c =  y + d * .75f;
		} else if (orientation == SE) {
			c =  y - d * .25f;
		} else if (orientation == NW) {
			c =  y + d * .25f;
		} else if (orientation == SW) {
			c =  y - d * .75f;
		}
		return c;	}
	
	private static class SAreaRoot extends HArea {
		public SAreaRoot() {
			super(null, L, 0f, 0f, NE, 0); 
		}
		
		public HArea[] getChildAreas() {
			HArea[] children = null;
			if (childrenRef != null) {
				children = childrenRef.get();
			}
			if (children == null) {
				// create 2 SAreas
				children = new SArea[2];
				children[0] = new SArea(this, L, 0f, 0f, DiagonalTurtle.NE, 0L);
				int squareSize = (1 << 16);
				children[1] = new SArea(this, L, squareSize, squareSize, DiagonalTurtle.SW, 1L << 31);
				
				childrenRef = new SoftReference<HArea[]>(children);
			}
			return children;
		}
		
		public Rectangle2D.Float getBoundsRect() {
			if (rect == null) {
				int squareSize = (1 << 16);
				rect = new Rectangle2D.Float(0, 0, squareSize, squareSize);
			}
			return rect;
		}
	}
	
}