/*
 * Created on 31-Mar-07
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.whois.WhoisManager.Source;

public class GetNetResult extends Result {

	private Net net;
	private String info;
	
	public GetNetResult(Net net, Source responseSource) {
		this(net, null, responseSource);
	}
	
	public GetNetResult(Net net, String info, Source responseSource) {
		super(responseSource);
		this.net = net;
		this.info = info; // can be null
	}
	
	public Net getNet() {
		return net;
	}
	
	public String getInfo() {
		return info;
	}
	
}
