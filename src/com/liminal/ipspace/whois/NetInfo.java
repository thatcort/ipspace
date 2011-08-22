/*
 * Created on 11-Apr-07
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import com.liminal.ipspace.data.Net;

public class NetInfo {

	private Net net;
	private String info;
	
	public NetInfo(Net net, String info) {
		this.net = net;
		this.info = info;
	}
	
	public Net getNet() {
		return net;
	}
	public String getInfo() {
		return info;
	}
}
