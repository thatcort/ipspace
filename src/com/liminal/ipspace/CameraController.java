/*
 * Created on 25-Feb-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace;

import com.liminal.p5.util.Camera;

public interface CameraController {

	/** return true if the camera moved at all */
	boolean updateCamera(Camera cam);
	
}
