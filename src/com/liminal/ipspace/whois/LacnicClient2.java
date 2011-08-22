/*
 * Created on 18-Apr-07
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import java.io.BufferedReader;

import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.data.Registry;
import com.liminal.ipspace.whois.Request.CallingMethod;

public class LacnicClient2 extends InternationalClient2 {

	public LacnicClient2(WhoisDB2 db) {
		super(Registry.LACNIC, db);
	}

	@Override
	protected Request getNetRequest(Net parent, String id) {
		return new Request(Registry.LACNIC, "", id, Request.NET_TYPE, id, CallingMethod.NET_METHOD, parent);
	}

	@Override
	protected Request getSubnetsRequest(Net parent) {
		String id = parent.getRange().getMinAddress().toString();
		return new Request(Registry.LACNIC, "", id, Request.NET_TYPE, id, CallingMethod.SUBNET_METHOD, parent);
	}
	
	@Override
	protected Request getNetInfoRequest(Net net) {
		String id = net.getRange().getCidrRanges()[0];
		return new Request(Registry.LACNIC, "", id, Request.NET_TYPE, id, CallingMethod.INFO_METHOD, net);
	}

	// TODO improve LACNIC to find other subnets by checking the query results
	

}
