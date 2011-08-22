/*
 * Created on 27-Nov-2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.p5.util;

public class XYZ {

	public float x, y, z;
	
	public XYZ() {}
	
	public XYZ(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public XYZ(XYZ xyz) {
		x = xyz.x;
		y = xyz.y;
		z = xyz.z;
	}
	
	public String toString() {
		return "XYZ: (" + x + ", " + y + ", " + z + ")";
	}

}
