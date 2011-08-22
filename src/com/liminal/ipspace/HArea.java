/*
 * Created on 30-Dec-2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.lang.ref.SoftReference;

public class HArea {

	public static final int NUM_LEVELS = 32; // i.e. 32 bits
	
	public static final char L = 'L';
	public static final char R = 'R';
	
	protected HArea parent;
	protected float x, y;
	protected int orientation;
	protected char symbol;
	protected int level;
	protected long linePos; // position on a line of length (2^NUM_LEVELS) -- maximum pos is thus (2^(NUM_LEVELS-1))-1
	protected SoftReference<HArea[]> childrenRef;
	protected Rectangle2D.Float rect;
	
	public static HArea getRoot() {
		return new HArea(null, L, 0f, 0f, Turtle.EAST, 0);
	}
	
	protected HArea(HArea parent, char symbol, float x, float y, int orientation, long linePos) {
		this.parent = parent;
		this.symbol = symbol;
		this.level = (parent == null ? 0 : parent.level + 1);
		this.x = x;
		this.y = y;
		this.orientation = orientation;
		this.linePos = linePos;
	}
	
	public HArea[] getChildAreas() {
		HArea[] children = null;
		if (childrenRef != null)
			children = childrenRef.get();
		if (children == null) { // create
			children = new HArea[4];
			long childPos = linePos;
			Turtle turtle = new Turtle(x, y, orientation);
			int childDist = getDist(level+1);
			int rangeDist = childDist * childDist;
			switch (symbol) {
			case L:
				//+ r - l l - r +
				turtle.turnLeft();
				children[0] = new HArea(this, R, turtle.getX(), turtle.getY(), turtle.getOrientation(), childPos);
				turtle.move(childDist);
				childPos += rangeDist;
				turtle.turnRight();
				children[1] = new HArea(this, L, turtle.getX(), turtle.getY(), turtle.getOrientation(), childPos);
				turtle.move(childDist);
				childPos += rangeDist;
				children[2] = new HArea(this, L, turtle.getX(), turtle.getY(), turtle.getOrientation(), childPos);
				turtle.move(childDist);
				childPos += rangeDist;
				turtle.turnRight();
				children[3] = new HArea(this, R, turtle.getX(), turtle.getY(), turtle.getOrientation(), childPos);
				break;
			case R:
				//- l + r r + l -
				turtle.turnRight();
				children[0] = new HArea(this, L, turtle.getX(), turtle.getY(), turtle.getOrientation(), childPos);
				turtle.move(childDist);
				childPos += rangeDist;
				turtle.turnLeft();
				children[1] = new HArea(this, R, turtle.getX(), turtle.getY(), turtle.getOrientation(), childPos);
				turtle.move(childDist);
				childPos += rangeDist;
				children[2] = new HArea(this, R, turtle.getX(), turtle.getY(), turtle.getOrientation(), childPos);
				turtle.move(childDist);
				childPos += rangeDist;
				turtle.turnLeft();
				children[3] = new HArea(this, L, turtle.getX(), turtle.getY(), turtle.getOrientation(), childPos);
				break;
			}
			childrenRef = new SoftReference<HArea[]>(children);
		}
		return children;
	}
	
	
	public HArea getParent() {
		return parent;
	}
	
	public long getMinAddress() {
		return linePos;
	}
	
	public int getLineDist() {
		return getDist(level);
	}
	
	public long getMaxAddress() {
		long ld = getLineDist();
		return linePos + (ld * ld) - 1;
	}
	
	protected static int getDist(int level) {
		return Math.max(1 << ((NUM_LEVELS/2) - level), 1);
	}
	
	public char getSymbol() { return symbol; }
	
	protected int getLevel() { return level; }
	
	public float getX() { return x; }
	public float getY() { return y; }
	public int getOrientation() { return orientation; }
	
	
	public Rectangle2D.Float getBoundsRect() {
		if (rect == null) {
			Turtle t = new Turtle(x, y, orientation);
			t.move(getLineDist());
			if (symbol == L)
				t.turnLeft();
			else
				t.turnRight();
			t.move(getLineDist());
			rect = new Rectangle2D.Float(Math.min(x, t.x), Math.min(y, t.y), Math.abs(x - t.x), Math.abs(y - t.y));
		}
		return rect;
	}
	
//	public Bounds getBounds() {
//		Bounds b = new Bounds(x, y, x, y);
//		Turtle t = new Turtle(x, y, orientation);
//		t.move(getLineDist());
//		if (symbol == L)
//			t.turnLeft();
//		else
//			t.turnRight();
//		t.move(getLineDist());
//		b.extendTo(t.x, t.y);
//		return b;
//	}
	
	public float getCenterX() {
		Rectangle2D.Float rect = getBoundsRect();
		return rect.x + (rect.width * .5f);
	}
	public float getCenterY() {
		Rectangle2D.Float rect = getBoundsRect();
		return rect.y + (rect.height * .5f);
	}
	
	public boolean containsPt(float x, float y) {
		Rectangle2D.Float rect = getBoundsRect();
		return (x >= rect.x && y >= rect.y && x <= rect.x + rect.width && y <= rect.y + rect.height);
	}
	
	public Area getAwtArea() {
		return new Area(getBoundsRect());
	}
}
