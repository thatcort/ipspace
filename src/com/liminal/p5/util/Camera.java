/*
 * Created on Jul 14, 2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.p5.util;

import processing.core.PApplet;

/**
 * This is an extension of the OCD Camera class to include methods
 * for retrieving camera settings and specifying the settings 
 * in absolute terms (rather than ones relative to the previous settings).
 * 
 * Data Type for manipulating the Processing viewport. There are
 * several ways to create a Camera, but all cameras are manipulated
 * the same way. The default camera position sits on the positive
 * z-axis. The default target position is at the world origin. The
 * default up direction is in the negative y. The default
 * field-of-view is PI/3 radians. The default aspect ratio is the
 * ratio of the applet width to the applet height. The default near 
 * clipping plane is placed a 1/10 x shot length. The default far
 * clipping plane is placed at 10x the shot length.
 *
 * @author  Kristian Linn Damkjer
 * @version OCD 1.0
 * @version Processing 0087
 * @since   OCD 1.0
 * @since   Processing 0087
 */ 
public class Camera {
    private static final float TWO_PI = (float) (2 * Math.PI);
    private static final float PI = (float) Math.PI;
    private static final float HALF_PI = (float)(Math.PI / 2);
    private static final float TOL = 0.00001f;

    //--- Attributes ----------
    private PApplet parent;

    /** Camera Orientation Information */
    private float azimuth,
                  elevation,
                  roll;

    /** Camera Look At Information */
    private float cameraX, cameraY, cameraZ,        // Camera Position
                  targetX, targetY, targetZ,        // Target Position
                  upX,     upY,     upZ,            // Up Vector
                  fov, aspect, nearClip, farClip;   // Field of View, Aspect Ratio, Clip Planes

    /** The length of the view vector */
    private float shotLength;

    /** distance differences between camera and target */
    private float dx, dy, dz;


    //--- Constructors --------

    /** 
     * Create a camera that sits on the z axis
     */
    public Camera(PApplet parent) {
        this(parent, (parent.height / 2f) / parent.tan(PI/6));
    }
    
    /** Gimme just a wee bit o' control, please. */
    public Camera(PApplet parent, float shotLength) {
        this(parent, 0, 0, shotLength);
    }

    /** Gimme just a little bit more control oughtta do it. */
    public Camera(PApplet parent, float cameraX, float cameraY, float cameraZ) {
        this(parent, cameraX, cameraY, cameraZ, 0, 0, 0);
    }
        
    // OK, I fancy myself a Director. Step aside.
    public Camera(PApplet parent,
                  float cameraX, float cameraY, float cameraZ,
                  float targetX, float targetY, float targetZ) {
        this(parent,
             cameraX, cameraY, cameraZ,
             targetX, targetY, targetZ,
                   0,       1,       0,
             (PI / 3), 1f * parent.width / parent.height, 0, 0);
        nearClip = shotLength / 10;
        farClip = shotLength * 10;
    }
    
    public Camera(PApplet parent,
    		float cameraX, float cameraY, float cameraZ,
    		float targetX, float targetY, float targetZ,
    		float upX, float upY, float upZ) {
        this(parent,
                cameraX, cameraY, cameraZ,
                targetX, targetY, targetZ,
                upX, upY, upZ,
                (PI / 3), 1f * parent.width / parent.height, 0, 0);
           nearClip = shotLength / 10;
           farClip = shotLength * 10;

    }
    
    // I mean it! Give me the controls already!
    public Camera(PApplet parent,
                  float cameraX, float cameraY, float cameraZ,
                  float targetX, float targetY, float targetZ,
                  float upX,     float upY,     float upZ,
                  float fov, float aspect, float nearClip, float farClip) {
        this.parent = parent;
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.upX = upX;
        this.upY = upY;
        this.upZ = upZ;
        this.fov = fov;
        this.aspect = aspect;
        this.nearClip = nearClip;
        this.farClip = farClip;
        
        dx = cameraX - targetX;
        dy = cameraY - targetY;
        dz = cameraZ - targetZ;
        
        shotLength = sqrt(dx * dx + dy * dy + dz * dz);
        azimuth    = atan2(dx, dz);
        elevation  = atan2(dy, sqrt(dz * dz + dx * dx));
        
        if (elevation > HALF_PI - TOL) {
          this.upY =  0;
          this.upZ = -1;
        }

        if (elevation < TOL - HALF_PI) {
          this.upY =  0;
          this.upZ =  1;
        }
        
    }

    //--- Behaviors ----------

    /** Send what this camera sees to the view port */
    public void feed() {
      parent.perspective(fov, aspect, nearClip, farClip);
      parent.camera(cameraX, cameraY, cameraZ,
             targetX, targetY, targetZ,
             upX,     upY,     upZ);
    }

    /** Aims the camera at the specified target */
    public void aim(float targetX, float targetY, float targetZ) {

        // Move the target
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;

        // Describe the new vector between the camera and the target
        dx = cameraX - targetX;
        dy = cameraY - targetY;
        dz = cameraZ - targetZ;

        // Describe the new azimuth and elevation for the camera
        shotLength = sqrt(dx * dx + dy * dy + dz * dz);
        azimuth    = atan2(dx, dz);
        elevation  = constrain(atan2(dy, sqrt(dz * dz + dx * dx)), TOL-HALF_PI, HALF_PI-TOL);
        
        // update the up vector
        updateUp();
    }
                
    /** Jumps the camera to the specified position */
    public void jump(float positionX, float positionY, float positionZ) {

        // Move the camera
        this.cameraX = positionX;
        this.cameraY = positionY;
        this.cameraZ = positionZ;

        // Describe the new vector between the camera and the target
        dx = cameraX - targetX;
        dy = cameraY - targetY;
        dz = cameraZ - targetZ;

        // Describe the new azimuth and elevation for the camera
        shotLength = sqrt(dx * dx + dy * dy + dz * dz);
        azimuth    = atan2(dx, dz);
        elevation  = constrain(atan2(dy, sqrt(dz * dz + dx * dx)), TOL-HALF_PI, HALF_PI-TOL);
        
        // update the up vector
        updateUp();
    }
                
    /** Changes the field of view between "fish-eye" and "close-up" */
    public void zoom(float amount) {
        fov = constrain(fov + amount, TOL, PI - TOL);
    }
    
    public void setZoom(float fov) {
    	this.fov = constrain(fov, TOL, PI - TOL);
    }

    /** Moves the camera and target simultaneously along the camera's X axis */
    public void truck(float amount) {

        // Calculate the camera's "X" vector
        float cXx = dy * upZ - dz * upY;
        float cXy = dx * upZ - dz * upX;
        float cXz = dx * upY - dy * upX;

        // Normalize the "X" vector so that it can be scaled
        float magnitude = sqrt(cXx * cXx + cXy * cXy + cXz * cXz);
        magnitude = (magnitude < TOL) ? 1 : magnitude;
        cXx /= magnitude;
        cXy /= magnitude;
        cXz /= magnitude;

        // Perform the truck, if any
        cameraX -= amount * cXx;
        cameraY -= amount * cXy;
        cameraZ -= amount * cXz;
        targetX -= amount * cXx;
        targetY -= amount * cXy;
        targetZ -= amount * cXz;
    }

    /** Moves the camera and target simultaneously along the camera's Y axis */
    public void boom(float amount) {
        // Perform the boom
        cameraX += amount * upX;
        cameraY += amount * upY;
        cameraZ += amount * upZ;
        targetX += amount * upX;
        targetY += amount * upY;
        targetZ += amount * upZ;
    }
    
    /** Moves the camera and target along the view vector */
    public void dolly(float amount) {
        float dirX = dx / shotLength;
        float dirY = dy / shotLength;
        float dirZ = dz / shotLength;
        
        cameraX += amount * dirX;
        cameraY += amount * dirY;
        cameraZ += amount * dirZ;
        targetX += amount * dirX;
        targetY += amount * dirY;
        targetZ += amount * dirZ;
    }
    
    /** Maintains orientation and shot length while moving the camera to the given coordinates */
    public void setTranslation(float x, float y, float z) {
    	float dx = x - cameraX;
    	float dy = y - cameraY;
    	float dz = z - cameraZ;
    	cameraX = x;
    	cameraY = y;
    	cameraZ = z;
    	targetX += dx;
    	targetY += dy;
    	targetZ += dz;
    }
    
    /** Rotates the camera about its X axis */
    public void tilt(float elevationOffset) {
    	setTilt(elevation - elevationOffset);
    }
    
    public void setTilt(float elev) {
    	
    	// Calculate the new elevation for the camera
    	elevation = constrain(elev, TOL-HALF_PI, HALF_PI-TOL);
    	
    	// Orbit to the new azimuth and elevation while maintaining the shot distance
    	targetX = cameraX - ( shotLength * sin(HALF_PI + elevation) * sin(azimuth));
    	targetY = cameraY - (-shotLength * cos(HALF_PI + elevation));
    	targetZ = cameraZ - ( shotLength * sin(HALF_PI + elevation) * cos(azimuth));
    	
    	// update the up vector
    	updateUp();
    }
    
    /** Rotates the camera about its Y axis */
    public void pan(float azimuthOffset) {
    	setPan(azimuth - azimuthOffset);
    }
    
    public void setPan(float azi) {
    	
    	// Calculate the new azimuth for the camera
    	azimuth = (azi + TWO_PI) % TWO_PI;
    	
    	// Orbit to the new azimuth and elevation while maintaining the shot distance
    	targetX = cameraX - ( shotLength * sin(HALF_PI + elevation) * sin(azimuth));
    	targetY = cameraY - (-shotLength * cos(HALF_PI + elevation));
    	targetZ = cameraZ - ( shotLength * sin(HALF_PI + elevation) * cos(azimuth));
    	
    	// update the up vector
    	updateUp();
    }
    
    /** Rotates the camera about its Z axis
     *  NOTE: rolls will NOT affect the azimuth, but WILL affect plans, trucks, and booms
     */
    public void roll(float amount) {
        // Change the roll amount
        roll += amount;
        
        // Update the up vector
        updateUp();
    }
    
    public void setRoll(float roll) {
    	this.roll = roll;
    	
    	updateUp();
    }

    /** Arcs the camera over (under) a center of interest along a set azimuth*/
    public void arc(float elevationOffset) {

        // Calculate the new elevation for the camera
//        elevation = constrain(elevation + elevationOffset, TOL-HALF_PI, HALF_PI-TOL);
    	elevation = constrain(elevation + elevationOffset, TOL-HALF_PI, 0);

        // Orbit to the new azimuth and elevation while maintaining the shot distance
        cameraX = targetX + ( shotLength * sin(HALF_PI + elevation) * sin(azimuth));
        cameraY = targetY + (-shotLength * cos(HALF_PI + elevation));
        cameraZ = targetZ + ( shotLength * sin(HALF_PI + elevation) * cos(azimuth));

        // update the up vector
        updateUp();
    }

    /** Circles the camera around a center of interest at a set elevation*/
    public void circle(float azimuthOffset) {

        // Calculate the new azimuth for the camera
        azimuth = (azimuth + azimuthOffset + TWO_PI) % TWO_PI;
System.out.println("azimuth: " + azimuth + ",   elevation: " + + elevation);
        // Orbit to the new azimuth and elevation while maintaining the shot distance
        cameraX = targetX + ( shotLength * sin(HALF_PI + elevation) * sin(azimuth));
        cameraY = targetY + (-shotLength * cos(HALF_PI + elevation));
        cameraZ = targetZ + ( shotLength * sin(HALF_PI + elevation) * cos(azimuth));

        // update the up vector
        updateUp();
    }
    
    public void circleXY(float angleOffset) {
System.out.println("up: " + getUpX() + ", " + getUpY() + ", " + getUpZ());
    	float angle = atan2(dy, dx);
    	if (dx < 0f)
    		angle = -angle;
    	angle += angleOffset;
    	
    	// Calculate the new xy-angle for the camera


        // Orbit to the new azimuth and elevation while maintaining the shot distance
        cameraX = targetX + ( shotLength * cos(HALF_PI + elevation) * cos(angle));
        cameraY = targetY + (-shotLength * cos(HALF_PI + elevation) * sin(angle));
////        cameraZ = targetZ + ( shotLength * sin(HALF_PI + elevation) * cos(azimuth));

        // update the up vector
//        updateUp();
        
    }

    /** Tumbles the camera about its center of interest */
    public void tumble(float azimuthOffset, float elevationOffset) {

        // Calculate the new azimuth and elevation for the camera
        azimuth = (azimuth + azimuthOffset + TWO_PI) % TWO_PI;
        elevation = constrain(elevation + elevationOffset, TOL-HALF_PI, HALF_PI-TOL);

        // Orbit to the new azimuth and elevation while maintaining the shot distance
        cameraX = targetX + ( shotLength * sin(HALF_PI + elevation) * sin(azimuth));
        cameraY = targetY + (-shotLength * cos(HALF_PI + elevation));
        cameraZ = targetZ + ( shotLength * sin(HALF_PI + elevation) * cos(azimuth));

        // update the up vector
        updateUp();
    }

    /** Allows the camera to freely look around its origin */
    public void look(float azimuthOffset, float elevationOffset) {

        // Calculate the new azimuth and elevation for the camera
        elevation = constrain(elevation - elevationOffset, TOL-HALF_PI, HALF_PI-TOL);
        azimuth = (azimuth - azimuthOffset + TWO_PI) % TWO_PI;

        // Orbit to the new azimuth and elevation while maintaining the shot distance
        targetX = cameraX - ( shotLength * sin(HALF_PI + elevation) * sin(azimuth));
        targetY = cameraY - (-shotLength * cos(HALF_PI + elevation));
        targetZ = cameraZ - ( shotLength * sin(HALF_PI + elevation) * cos(azimuth));

        // update the up vector
        updateUp();
    }
    
    /** sets the look-at target */
    public void lookAt(float x, float y, float z) {
    	targetX = x;
    	targetY = y;
    	targetZ = z;
    	
		dx = cameraX - targetX;
		dy = cameraY - targetY;
		dz = cameraZ - targetZ;
		shotLength = sqrt(dx*dx + dy*dy + dz*dz);
		
		updateUp();
    }

    /** Moves the camera and target simultaneously in the camera's X-Y plane */
    public void track(float xOffset, float yOffset) {
        // Perform the truck, if any
        truck(xOffset);

        // Perform the boom, if any
        boom(yOffset);
    }
    
	public void moveForward(float distance, boolean moveTarget) {
		float moveX = distance * -dx / shotLength;
		float moveY = distance * -dy / shotLength;
		float moveZ = distance * -dz / shotLength;
		cameraX += moveX;
		cameraY += moveY;
		cameraZ += moveZ;
		if (moveTarget) {
			targetX += moveX;
			targetY += moveY;
			targetZ += moveZ;
		} else {
			dx = cameraX - targetX;
			dy = cameraY - targetY;
			dz = cameraZ - targetZ;
			shotLength = sqrt(dx*dx + dy*dy + dz*dz);
		}
	}
	
	public void setShotLength(float dist) {
		float ratio = dist / shotLength;
		dx *= ratio;
		dy *= ratio;
		dz *= ratio;
		targetX = cameraX - dx;
		targetY = cameraY - dy;
		targetZ = cameraZ - dz;
		shotLength = dist;
	}
    
    public void setNearFarClip(float near, float far) {
    	nearClip = near;
    	farClip = far;
    }
    
    public float[] getTranslation() { return new float[] { cameraX, cameraY, cameraZ }; }
    public float getCameraX() { return cameraX; }
    public float getCameraY() { return cameraY; }
    public float getCameraZ() { return cameraZ; }
    public float[] getTarget() { return new float[] { targetX, targetY, targetZ }; }
    public float getTargetX() { return targetX; }
    public float getTargetY() { return targetY; }
    public float getTargetZ() { return targetZ; }
    public float getUpX() { return upX; }
    public float getUpY() { return upY; }
    public float getUpZ() { return upZ; }
    public float[] getUp() { return new float[] { upX, upY, upZ }; }
    public float getFOV() { return fov; }
    public float getShotLength() { return shotLength; }
    public float getNearClip() { return nearClip; }
    public float getFarClip() { return farClip; }
    

    //---- Helpers -------------------------------------------------------------
    /** */
    private void updateUp() {

        // Describe the new vector between the camera and the target
        dx = cameraX - targetX;
        dy = cameraY - targetY;
        dz = cameraZ - targetZ;

        // Calculate the new "up" vector for the camera
        upX = -dx * dy;
        upY =  dz * dz + dx * dx;
        upZ = -dz * dy;

        // Normalize the "up" vector
        float magnitude = sqrt(upX * upX + upY * upY + upZ * upZ);
        magnitude = (magnitude < TOL) ? 1 : magnitude;
        upX /= magnitude;
        upY /= magnitude;
        upZ /= magnitude;
        
        // Calculate the roll if there is one
        if (roll != 0) {

            // Calculate the camera's "X" vector
            float cXx = dy * upZ - dz * upY;
            float cXy = dx * upZ - dz * upX;
            float cXz = dx * upY - dy * upX;

            // Normalize the "X" vector so that it can be scaled
            magnitude = sqrt(cXx * cXx + cXy * cXy + cXz * cXz);
            magnitude = (magnitude < 0.001) ? 1 : magnitude;
            cXx /= magnitude;
            cXy /= magnitude;
            cXz /= magnitude;

            // Perform the roll
            cXx *= sin(roll);        
            cXy *= sin(roll);        
            cXz *= sin(roll);        
            upX *= cos(roll);
            upY *= cos(roll);
            upZ *= cos(roll);
            upX += cXx;
            upY += cXy;
            upZ += cXz;
        }
    }

    //--- Simple Hacks ----------
    private final float radians(float a) {
      return parent.radians(a);
    }

    private final float sin(float a) {
      return parent.sin(a);
    }

    private final float cos(float a) {
      return parent.cos(a);
    }

    private final float tan(float a) {
      return parent.tan(a);
    }

    private final float sqrt(float a) {
      return parent.sqrt(a);
    }

    private final float atan2(float y, float x) {
      return parent.atan2(y, x);
    }

    private final float degrees(float a) {
      return parent.degrees(a);
    }

    private final float constrain(float v, float l, float u) {
      return parent.constrain(v, l, u);
    }
}