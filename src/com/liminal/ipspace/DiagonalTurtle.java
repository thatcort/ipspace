/*
 * File:      DiagonalTurtle.java
 * Created:   29-Mar-2006
 * Author:    bcort
 *
 * Copyright (c) 2003-2004 by Oculus Info Inc.  All rights reserved.
 *
 * $Id$
 *
 */
package com.liminal.ipspace;

import com.liminal.p5.util.XYZ;

public class DiagonalTurtle {

	public static final int NW = 0;
	public static final int SW = 1;
	public static final int SE = 2;
	public static final int NE = 3;
	
	private final float oneOversqrt2 = (float) (1d / Math.sqrt(2));
	
	protected float x, y;
	protected int orientation;
	
	public DiagonalTurtle() {}
	
	public DiagonalTurtle(float x, float y, int orientation) {
		this.x = x;
		this.y = y;
		this.orientation = orientation;
	}
	
	public DiagonalTurtle(DiagonalTurtle turtle) {
		this.x = turtle.x;
		this.y = turtle.y;
		this.orientation = turtle.orientation;
	}
	
	public void moveTo(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public void moveTo(XYZ xyz) {
		x = xyz.x;
		y = xyz.y;
	}

	public void move(float dist) {
		float axisDist = dist * oneOversqrt2;
		moveManhattan(axisDist);
	}
	
	public void moveManhattan(float axisDist) {
		switch (orientation) {
		case NW:
			x -= axisDist;
			y += axisDist;
			break;
		case NE:
			x += axisDist;
			y += axisDist;
			break;
		case SW:
			x -= axisDist;
			y -= axisDist;
			break;
		case SE:
			x += axisDist;
			y -= axisDist;
			break;
		}
	}
	
	public void turnRight() {
		orientation--;
		if (orientation < 0)
			orientation += 4;
	}
	
	public void turnLeft() {
		orientation = (orientation + 1) % 4;
	}
	
	public int getOrientation() {
		return orientation;
	}
	
	public void setOrientation(int or) {
		orientation = or;
	}
	
	public XYZ getPosition() {
		return new XYZ(x, y, 0f);
	}
	
	public float getX() { return x; }
	public float getY() { return y; }
	

}
