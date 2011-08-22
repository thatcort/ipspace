/*
 * Created on 6-Nov-2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.p5.util;

import processing.core.PApplet;

public class PickState {

	private PApplet papplet;
	private float[] lookVec;
	private float distanceEyeMousePlane;
	private float[] eyePos;
	private float[] left; // direction of left
	private float[] up; // direction of up
	
	
	public PickState(PApplet papplet, Camera cam) {
		this.papplet = papplet;
		
		eyePos = cam.getTranslation();
		lookVec = cam.getTarget(); // becomes the direction of view from the camera 'eye'
		lookVec[0] -= eyePos[0];
		lookVec[1] -= eyePos[1];
		lookVec[2] -= eyePos[2];
		normalize(lookVec);
		
		up = cam.getUp();
		normalize(up);
		
		left = cross3(up, lookVec);
		
		distanceEyeMousePlane = (papplet.height / 2f) / (float) Math.tan(cam.getFOV() / 2f);
	}
	
	
	public float[] pick(int normalAxis, float height) {
		return pick(normalAxis, height, papplet.mouseX, papplet.mouseY);
	}
	
	public float[] pick(int normalAxis, float height, float screenX, float screenY) {
		// vector from eye to point mouse is on
		float[] pickRay = multiply(lookVec, distanceEyeMousePlane);
		float offsetX = screenX - (papplet.width / 2f);
		float offsetY = screenY - (papplet.height / 2f);
		float[] leftComp = multiply(left, -offsetX);
		float[] upComp = multiply(up, offsetY);
		add(pickRay, leftComp, pickRay);
		add(pickRay, upComp, pickRay);
		
		float[] result = new float[3];
		pick(eyePos, pickRay, normalAxis, height, result);
		return result;
	}
	
	/** Returns the squared distance from the eye to the point */
	public float getDistance2(float x, float y, float z) {
		float dx = eyePos[0] - x;
		float dy = eyePos[1] - y;
		float dz = eyePos[2] - z;
		return dx*dx + dy*dy + dz*dz;
	}
	
	public float getDistance2(float[] p) {
		return getDistance2(p[0], p[1], p[2]);
	}
	
	/**
	 * 
	 * @param pos 
	 * @param dir
	 * @param normalAxis
	 * @param height
	 * @param result
	 */
	private static void pick(float[] pos, float[] dir, int normalAxis, float height, float[] result) {
		
		float t = (height - pos[normalAxis]) / dir[normalAxis];
		for (int i=1; i < 3; i++) {
			int ind = (normalAxis + i) % 3;
			result[ind] = pos[ind] + t*dir[ind];
		}
		result[normalAxis] = height;
	}
	
	
	private static float[] cross3(float[] v1, float[] v2) {
		float[] vOut = new float[3];
		cross3(v1, v2, vOut);
		return vOut;
	}
	private static void cross3(float[] v1, float[] v2, float[] vOut) {
		vOut[0] = v1[1]*v2[2] - v2[1]*v1[2];
		vOut[1] = v1[2]*v2[0] - v2[2]*v1[0];
		vOut[2] = v1[0]*v2[1] - v2[0]*v1[1];
	}
	
	private static void normalize(float[] vec) {
		float len = 0f;
		for (int i=0; i < vec.length; i++)
			len += vec[i] * vec[i];
		len = (float) Math.sqrt(len);
		for (int i=0; i < vec.length; i++)
			vec[i] /= len;
	}
	
	private static float[] multiply(float[] v1, float s) {
		float[] vOut = new float[v1.length];
		multiply(v1, s, vOut);
		return vOut;
	}
	
	private static void multiply(float[] v1, float s, float[] vOut) {
		for (int i=0; i < v1.length; i++)
			vOut[i] = v1[i] * s;		
	}
	
	private static void add(float[] v1, float[] v2, float[] vOut) {
		for (int i=0; i < v1.length; i++)
			vOut[i] = v1[i] + v2[i];
	}
}
