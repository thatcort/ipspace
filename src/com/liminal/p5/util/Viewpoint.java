/*
 * Created on 11-Dec-2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.p5.util;

public class Viewpoint {

	private float lat;
	private float lon;
	private float fx, fy, fz; // focus
	private float dist;

	private float x, y, z; // position
	
	public Viewpoint(float lat, float lon, float focusX, float focusY, float focusZ, float dist) {
		this.lat = lat;
		this.lon = lon;
		this.fx = focusX;
		this.fy = focusY;
		this.fz = focusZ;
		this.dist = dist;
		
		computePosition();
	}
	
	private void computePosition() {
		x = fx + dist * (float) (Math.cos(lon) * (1d - Math.sin(lat)));
		y = fy + dist * (float) (Math.sin(lon) * (1d - Math.sin(lat)));
		z = fz + dist * (float) Math.sin(lat);
	}
	
	public float getX() { return x; }
	public float getY() { return y; }
	public float getZ() { return z; }
	
}
