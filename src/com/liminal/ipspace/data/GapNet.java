/*
 * Created on 22-Mar-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.data;

public class GapNet extends Net {

	public GapNet(Net parent, AddressRange range) {
		super(parent, range, parent.getRegistry(), null);
	}

	public boolean isAllocated() { return false; }

}
