/*
 * Created on 29-Jan-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.data.Registry;

public class RIRDelegationResult {

	private Registry registry;
	private Net[] nets;
	
	public RIRDelegationResult(Registry registry, Net[] nets) {
		this.registry = registry;
		this.nets = nets;
	}

	public Net[] getNets() {
		return nets;
	}

	public Registry getRegistry() {
		return registry;
	}

}
