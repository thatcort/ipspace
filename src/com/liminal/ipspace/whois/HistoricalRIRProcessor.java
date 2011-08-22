/*
 * Created on 19-Mar-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import com.liminal.ipspace.data.AddressRange;
import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.data.Registry;

class HistoricalRIRProcessor {


	Net[] processRIRDelegations(Registry registry, AddressRange[] ranges, Net internet) {
		Net[] nets = new Net[ranges.length];
		for (int i = 0; i < ranges.length; i++) {
			nets[i] = new Net(internet, ranges[i], registry, ranges[i].toString());
		}
		return nets;
	}
	
}
