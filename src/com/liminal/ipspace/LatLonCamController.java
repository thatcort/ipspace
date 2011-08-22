/*
 * Created on 25-Feb-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace;

import static processing.core.PConstants.*;

import com.liminal.p5.util.Camera;
import com.liminal.p5.util.PickState;

public class LatLonCamController implements CameraController {

	private IPSpace ipspace;
	
	private float maxZ;
	private float minZ;

	private float moveAmount = 500f;

	private float dx, dy;
	
	public LatLonCamController(IPSpace ipspace, float minZ, float maxZ) {
		this.ipspace = ipspace;
		this.minZ = -minZ;
		this.maxZ = -maxZ;

	}

	public boolean updateCamera(Camera cam) {
		dx = ipspace.mouseX - ipspace.pmouseX;
		dy = ipspace.mouseY - ipspace.pmouseY;

		boolean moved = false;
		moved |= pan(cam);
		moved |= rotate(cam);
		moved |= zoom(cam);
		
		return moved;
	}

	
	
	private boolean zoom(Camera cam) {
		if (ipspace.mousePressed && ipspace.mouseButton == LEFT) {
//			float moveZ = moveAmount * (cam.getTargetZ() - cam.getCameraZ()) / cam.getShotLength();
			boolean zoomOut = ipspace.keyPressed && ipspace.key == CODED && ipspace.keyCode == SHIFT;
//			float t;
//			// check against min/max Z:
//			if (zoomOut)
//				t = (maxZ - cam.getCameraZ()) / moveZ; // check against maxZ
//			else
//				t = (cam.getCameraZ() - minZ) / moveZ; // check against minZ
//			if (t < 0f) t= -t;
//			if (t > 1f)
//				t = 1f;
//			cam.moveForward(moveAmount * (zoomOut ? -t : t), false);
	
//			cam.moveForward((zoomOut ? -cam.getShotLength()+minZ : cam.getShotLength()-minZ) * .05f, false);
			float dc = (zoomOut ? -cam.getShotLength() : cam.getShotLength()) * .02f;
//System.out.println("cam move: " + dc);
			if (zoomOut || cam.getCameraZ() < minZ) {
				cam.moveForward(dc, true); // true is faster than false, since it doesn't recalculate the shot length, which we do in the next line anyway.
				updateShotLength(cam);
			}
//			System.out.println("shotLength: " + cam.getShotLength());
//			cam.zoom(zoomOut ? -1f : 1f);
			
			return true;
		}
		return false;
	}
	
	private boolean pan(Camera cam) {
		if (ipspace.mousePressed && ipspace.mouseButton == LEFT) {
			float panFactor = cam.getCameraZ() * 100f / maxZ;
			cam.track(-dx * panFactor, -dy * panFactor);
			return true;
		}
		return false;
	}
	
	private float rotateFactor = .001f;
	
	private boolean rotate(Camera cam) {
		if (ipspace.mousePressed && ipspace.mouseButton == RIGHT) {
			cam.arc(dy * rotateFactor);
			
			updateShotLength(cam);
			
//			cam.circle(dx * rotateFactor);
//			cam.circleXY(dx * rotateFactor);
//			cam.tilt(dx * rotateFactor);
//			cam.roll(dx * rotateFactor);
//			cam.tumble(dx * rotateFactor, dy * rotateFactor);
			
			return true;
		}
		return false;
	}
	
	private void updateShotLength(Camera cam) {
		PickState ps = ipspace.getPickState();		
		float[] pick = ps.pick(2, 0f, ipspace.width * .5f, ipspace.height * .5f);
		float dist = (float) Math.sqrt(ps.getDistance2(pick));
		cam.setShotLength(dist);
	}
	
	
}

