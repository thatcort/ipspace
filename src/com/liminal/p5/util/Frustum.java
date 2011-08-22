/*
 * Created on 15-Feb-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.p5.util;

import javolution.util.FastMap;
import processing.core.PGraphics3D;
import processing.core.PMatrix;
import processing.core.PMatrix3D;

import com.liminal.p5.util.Plane.Halfspace;

public class Frustum {

	public static enum Containment {
		INSIDE,
		INTERSECT,
		OUTSIDE
	}
	
	public static final int LEFT_INDEX = 0;
	public static final int RIGHT_INDEX = 1;
	public static final int TOP_INDEX = 2;
	public static final int BOTTOM_INDEX = 3;
	public static final int NEAR_INDEX = 4;
	public static final int FAR_INDEX = 5;
	

	private Plane[] planes = new Plane[6];
	
	private FastMap<QTNode, Frustum.Containment> qtContainmentMap = new FastMap<QTNode, Containment>();

	public Frustum(PGraphics3D g) {
//		PMatrix mvMat = g.modelview;
//		PMatrix projMat = g.projection;
//		
//		PMatrix combo = new PMatrix(projMat);
//		combo.apply(mvMat);
		PMatrix3D mvMat = g.modelview;
		PMatrix3D projMat = g.projection;
		
		PMatrix3D combo = new PMatrix3D(projMat);
		combo.apply(mvMat);
		
		Plane leftPlane = new Plane(
				combo.m30 + combo.m00,
				combo.m31 + combo.m01,
				combo.m32 + combo.m02,
				combo.m33 + combo.m03);
		
		Plane rightPlane = new Plane(
				combo.m30 - combo.m00,
				combo.m31 - combo.m01,
				combo.m32 - combo.m02,
				combo.m33 - combo.m03);
		
		Plane topPlane = new Plane(
				combo.m30 - combo.m10,
				combo.m31 - combo.m11,
				combo.m32 - combo.m12,
				combo.m33 - combo.m13);
		
		Plane bottomPlane = new Plane(
				combo.m30 + combo.m10,
				combo.m31 + combo.m11,
				combo.m32 + combo.m12,
				combo.m33 + combo.m13);
		
		Plane nearPlane = new Plane(
				combo.m30 + combo.m20,
				combo.m31 + combo.m21,
				combo.m32 + combo.m22,
				combo.m33 + combo.m23);
		
		Plane farPlane = new Plane(
				combo.m30 - combo.m20,
				combo.m31 - combo.m21,
				combo.m32 - combo.m22,
				combo.m33 - combo.m23);
		
//		// account for the left-handed axes
//		leftPlane.c = -leftPlane.c;
//		rightPlane.c = -rightPlane.c;
//		topPlane.c = -topPlane.c;
//		bottomPlane.c = -bottomPlane.c;
//		nearPlane.c = -nearPlane.c;
//		farPlane.c = -farPlane.c;
		
		planes[LEFT_INDEX] = leftPlane;
		planes[RIGHT_INDEX] = rightPlane;
		planes[TOP_INDEX] = topPlane;
		planes[BOTTOM_INDEX] = bottomPlane;
		planes[NEAR_INDEX] = nearPlane;
		planes[FAR_INDEX] = farPlane;
		
	}
	
	public Plane[] getPlanes() {
		return planes;
	}
	
	public Plane getPlane(int index) {
		return planes[index];
	}
	
	public Containment contains(QTNode qt) {
		Containment c = qtContainmentMap.get(qt);
		if (c == null) {
			QTNode pqt = qt.getParent();
			Containment pc = null;
			if (pqt != null)
				pc = contains(pqt);
			if (pc != null && pc != Containment.INTERSECT) {
				c = pc;
//				qtContainmentMap.put(qt, c);
			} else { // instersection
				// first check the fast, but crude sphere test
				c = containsSphere(qt.getBoundingSphere());
				if (c == Containment.INTERSECT) {
					// intersected the sphere, so check the more precise rectangle test
					c = containsXYRect(qt.getMinX(), qt.getMinY(), qt.getMaxX(), qt.getMaxY());
				}
			}
			qtContainmentMap.put(qt, c);
		}
		return c;
	}
	
	public Containment containsSphere(BSphere sphere) {
		for (int i=0; i < 6; i++) {
			float dist = planes[i].distanceToPoint(sphere.x, sphere.y, sphere.z);
			if (dist < -sphere.r) {
//System.out.println("frustum rejected sphere on plane: " + i);
				return Containment.OUTSIDE;
			}
			if (dist <  sphere.r)
				return Containment.INTERSECT;
		}
		return Containment.INSIDE;
	}
	
	/** Tests containment for a rectangle on the z=0 plane */
	public Containment containsXYRect(float minX, float minY, float maxX, float maxY) {
		
		int totalIn = 0;
		
		for (int p=0; p < 6; p++) {
			
			int inCount = 4;
			int ptIn = 1; // used as boolean
			
			for (int v=0; v < 4; v++) {
				// test this point against the plane
				float x = (v == 0 || v == 3) ? minX : maxX;
				float y = (v < 2) ? minY : maxY;
				Halfspace hs = planes[p].classifyPoint(x, y, 0f);
				if (hs == Plane.Halfspace.NEGATIVE) {
					ptIn = 0;
					inCount--;
				}
			}
			
			// were all pts behind the plane?
			if (inCount == 0)
				return Containment.OUTSIDE;
			
			totalIn += ptIn;
		}
		
		// if totalIn = 6, then all points were inside all planes
		if (totalIn == 6)
			return Containment.INSIDE;
		
		// must be partly inside
		return Containment.INTERSECT;
	}

}
 