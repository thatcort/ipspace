/*
 * Created on 27-Nov-2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace;

import com.liminal.p5.util.XYZ;

public class Turtle {
	
	public static final int NORTH = 0;
	public static final int WEST = 1;
	public static final int SOUTH = 2;
	public static final int EAST = 3;
	
	
	protected float x, y;
	protected int orientation;
	
	public Turtle() {}
	
	public Turtle(float x, float y, int orientation) {
		this.x = x;
		this.y = y;
		this.orientation = orientation;
	}
	
	public Turtle(Turtle turtle) {
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
		switch (orientation) {
		case WEST:
			x -= dist;
			break;
		case EAST:
			x += dist;
			break;
		case NORTH:
			y += dist;
			break;
		case SOUTH:
			y -= dist;
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
