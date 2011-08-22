/*
 * Created on 19-Nov-2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.p5.util;


public class Bounds {

	private float minX, minY, maxX, maxY; // the bounds of the net area. Defines a square.
	
	private boolean inclusive = true;
	
	public Bounds(float minX, float minY, float maxX, float maxY) {
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
	}
	
	public void extendTo(float x, float y) {
		if (x > maxX)
			maxX = x;
		if (x < minX)
			minX = x;
		if (y > maxY)
			maxY = y;
		if (y < minY)
			minY = y;
	}
	
	public boolean contains(XYZ p) {
		if (inclusive)
			return p.x >= minX && p.x <= maxX && p.y >= minY && p.y <= maxY;
		return p.x >= minX && p.x < maxX && p.y >= minY && p.y < maxY;
	}
	
	public boolean contains(float[] p) {
		return contains(p[0], p[1]);
	}
	
	public boolean contains(float x, float y) {
		if (inclusive)
			return x >= minX && x <= maxX && y >= minY && y <= maxY;
		return x >= minX && x < maxX && y >= minY && y < maxY;

	}
	
	public String toString() {
		return "Bounds: " + "(" + minX + ", " + minY + ")..(" + maxX + ", " + maxY + ")";
	}
	
	public XYZ getMin() { return new XYZ(minX, minY, 0f); }
	public XYZ getMax() { return new XYZ(maxX, maxY, 0f); }
	
	public float getMinX() { return minX; }
	public float getMinY() { return minY; }
	public float getMaxX() { return maxX; }
	public float getMaxY() { return maxY; }
	
	public float getSizeX() { return maxX - minX; }
	public float getSizeY() { return maxY - minY; }
}
