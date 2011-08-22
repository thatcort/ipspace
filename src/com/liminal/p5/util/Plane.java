/*
 * Created on 15-Feb-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.p5.util;

/** Plane equation of the form ax + by + cz + d = 0 */
public class Plane {
	
	public static enum Halfspace {
		NEGATIVE,
		ON_PLANE,
		POSITIVE
	}
	
	public static float epsilon = 0.01f;
	
	public float a, b, c, d; // normalized 
	
	public Plane() {
	}
	
	public Plane(float a, float b, float c, float d) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		float mag = (float) Math.sqrt(a * a + b * b + c * c);
		a /= mag;
		b /= mag;
		c /= mag;
		d /= mag;
	}
	
	public float distanceToPoint(float[] pt) {
		return distanceToPoint(pt[0], pt[1], pt[2]);
	}
	
	public float distanceToPoint(float x, float y, float z) {
		return a * x + b * y + c * z + d;
	}
	
	public Halfspace classifyPoint(float x, float y, float z) {
		float d = distanceToPoint(x, y, z);
		if (d < -epsilon) return Halfspace.NEGATIVE;
		if (d > epsilon) return Halfspace.POSITIVE;
		return Halfspace.ON_PLANE;
	}

}
