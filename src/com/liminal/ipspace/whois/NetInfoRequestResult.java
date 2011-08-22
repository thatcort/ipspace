/*
 * Created on 13-Mar-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import com.liminal.ipspace.whois.WhoisManager.Source;

class NetInfoRequestResult {

	Request request;
	String info;
	Source responseSource; // one of local or remote
	String response; // raw registry response

	public NetInfoRequestResult(Request request, String response, Source responseSource, String info) {
		this.request = request;
		this.response = response;
		this.responseSource = responseSource;
		this.info = info;
	}

}
