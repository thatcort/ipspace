/*
 * Created on 2-Mar-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.whois.WhoisManager.Source;

public class GetNetInfoResult extends Result {

	private Net net;
	private String info;
	
	public GetNetInfoResult(Net net, String info, Source responseSource) {
		super(responseSource);
		this.net = net;
		this.info = info;
	}

	public String getInfo() {
		return info;
	}

	public Net getNet() {
		return net;
	}

}
