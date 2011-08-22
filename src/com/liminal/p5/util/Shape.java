/*
 * Created on 19-Jan-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.p5.util;

import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PGraphics3D;

public class Shape {

	private int type;
	private float[] xs = new float[0];
	private float[] ys = new float[0];
	private float[] zs = new float[0];
//	private BitSet fillSet = new BitSet();
//	private int[] fills = new int[0];
//	private BitSet strokeSet = new BitSet();
//	private int[] strokes = new int[0];
	
	public Shape(int type) {
		this.type = type;
	}
	
	public Shape(int type, int numVertices) {
		this.type = type;
		setNumVertices(numVertices);
	}

	
	public void setNumVertices(int nv) {
		xs = resizeArray(xs, nv);
		ys = resizeArray(ys, nv);
		zs = resizeArray(zs, nv);
//		if (nv < fillSet.length())
//			fillSet.clear(nv, fillSet.length());
//		fills = resizeArray(fills, nv);
//		if (nv < strokeSet.length())
//			strokeSet.clear(nv, strokeSet.length());
//		strokes = resizeArray(strokes, nv);
	}
	
	public int getNumVertices() { return xs.length; }
	
	public void setVertex(int vert, float x, float y, float z) {
		xs[vert] = x;
		ys[vert] = y;
		zs[vert] = z;
	}
	
//	public void setFill(int vert, int color) {
//		fills[vert] = color;
//		fillSet.set(vert);
//	}
//	public void clearFill(int vert) {
//		fillSet.clear(vert);
//	}
//	public void setStroke(int vert, int color) {
//		strokes[vert] = color;
//		strokeSet.set(vert);
//	}
//	public void clearStroke(int vert) {
//		strokeSet.clear(vert);
//	}
	
	public void draw(PGraphics3D g) {
		draw(g, true, true);
	}
	
	private void draw(PGraphics3D g, boolean stroke, boolean fill) {
		g.beginShape(type);
		
		for (int i=0; i < xs.length; i++) {
//			if (strokeSet.get(i) && stroke)
//				g.stroke(strokes[i]);
//			if (fillSet.get(i) && fill)
//				g.fill(fills[i]);
//float x = xs[i] * .5f;
//float y = ys[i] * .5f;
//float z = zs[i] * .5f;
//g.vertex(x, y, z);
			g.vertex(xs[i], ys[i], zs[i]);
		}
		
		g.endShape(PConstants.CLOSE);
	}
	
	public void draw2D(PGraphics g) {
		g.beginShape(type);
		
		for (int i=0; i < xs.length; i++) {
//			if (strokeSet.get(i) && stroke)
//				g.stroke(strokes[i]);
//			if (fillSet.get(i) && fill)
//				g.fill(fills[i]);
//float x = xs[i] * .5f;
//float y = ys[i] * .5f;
//float z = zs[i] * .5f;
//g.vertex(x, y, z);
			g.vertex(xs[i], ys[i]);
		}
		
		g.endShape(PConstants.CLOSE);
	}
	
	private float[] resizeArray(float[] fa, int newSize) {
		float[] na = new float[newSize];
		System.arraycopy(fa, 0, na, 0, Math.min(fa.length, na.length));
		return na;
	}
	private int[] resizeArray(int[] ia, int newSize) {
		int[] na = new int[newSize];
		System.arraycopy(ia, 0, na, 0, Math.min(ia.length, na.length));
		return na;
	}

	
}
