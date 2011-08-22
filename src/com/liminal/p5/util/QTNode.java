/*
 * Created on 18-Feb-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.p5.util;

import java.awt.geom.Rectangle2D;

/** Quad Tree node */
public class QTNode {

	private static float MIN_SIZE = 0f;
	
	public static float getMinSize() { return MIN_SIZE; }
	public static void setMinSize(float minSize) {
		MIN_SIZE = minSize;
	}
	
	private QTNode parent;
	private float minX, minY;
	private float maxX, maxY;
	private float size; // length of a side
	
	private QTNode c00, c10, c11, c01;
	
	private BSphere boundingSphere = null; 
	
	public QTNode(QTNode parent, float x, float y, float size) {
		this.parent = parent;
		this.minX = x;
		this.minY = y;
		this.size = size;
		maxX = x + size;
		maxY = y + size;
		
		float hs = size * .5f;
		float r = (float) Math.sqrt(2 * hs * hs);
		boundingSphere = new BSphere(x + hs, y + hs, 0f, r);
	}
	
	public BSphere getBoundingSphere() { return boundingSphere; }
	
	public QTNode getParent() { return parent; }
	
	/** children are numbered according to the bits xy, so 00 = x=0,y=0 (lower left); 10 = x=1,y=0 (lower right), etc. weird, but it kinda makes sense */
	public QTNode getChild(int i) {
		QTNode c = null;
		switch (i) {
		case 0:
			c = c00;
			if (c == null) { 
				c = new QTNode(this, minX, minY, (maxX - minX) * .5f);
				c00 = c;
			}	
			break;
		case 2:
			c = c10;
			if (c == null) {
				float s = (maxX - minX) * .5f;
				c = new QTNode(this, minX + s, minY, s);
				c10 = c;
			}	
			break;
		case 3:
			c = c11;
			if (c == null) { 
				float s = (maxX - minX) * .5f;
				c = new QTNode(this, minX + s, minY + s, s);
				c11 = c;
			}	
			break;
		case 1:
			c = c01;
			if (c == null) { 
				float s = (maxX - minX) * .5f;
				c = new QTNode(this, minX, minY + s, s);
				c01 = c;
			}
			break;
		default:
			throw new IllegalArgumentException("Invalid quad tree child index: " + i + ". Must be between 0 and 3.");
		}
		return c;
	}
	
	public float getMinX() { return minX; }
	public float getMinY() { return minY; }
	public float getMaxX() { return maxX; }
	public float getMaxY() { return maxY; }
	
	public float getCenterX() { return minX + (size * .5f); }
	public float getCenterY() { return minY + (size * .5f); }
	
	public float getSize() { return size; }

	public boolean intersects(float ox, float oy) {
		return (ox >= minX) && (ox <= maxX) && (oy >= minY) && (oy <= maxY);
	}
	
	public boolean contains(float x, float y) {
		return x >= minX && x <= maxX && y >= minY && y <= maxY;
	}
	
	public boolean contains(Rectangle2D rect) {
		return rect.getMinX() >= minX && rect.getMinY() >= minY && rect.getMaxX() <= maxX && rect.getMaxY() <= maxY;
	}

	public QTNode getSmallestContainingNode(Rectangle2D rect) {
		// if do not contain it, then go up the hierarchy
		if (!contains(rect)) {
			if (parent == null)
				return null; // shouldn't happen
			return parent.getSmallestContainingNode(rect);
		}
		
		// check if small enough to be contained by a child:
		float hs = size * .5f;
		if (hs <= MIN_SIZE || rect.getWidth() > hs || rect.getHeight() > hs)
			return this;
		
		float hx = minX + hs;
		float hy = minY + hs;

		// check for overlap over children boundaries
		if ((rect.getMinX() < hx && rect.getMaxX() > hx)
				|| (rect.getMinY() < hy && rect.getMaxY() > hy))
			return this;
		
		int childIndex = 0x0;
		if (rect.getMinX() >= hx)
			childIndex |= 0x2;
		if (rect.getMinY() >= hy)
			childIndex |= 0x1;
		QTNode cnode = getChild(childIndex);
		return cnode.getSmallestContainingNode(rect);
	}

}
