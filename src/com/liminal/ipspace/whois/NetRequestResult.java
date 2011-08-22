/*
 * Created on 29-Jan-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.whois.WhoisManager.Source;

/** This class is used within the whois package to contain the result and metadata about the result for whois searches (local or remote) */
class NetRequestResult {

	Request request;
	Net[] nets;
//	Reader response;
	Source responseSource; // one of local or remote
	String response;
	
	public NetRequestResult(Request request, String response, Source responseSource) {
		this.request = request;
		this.response = response;
		this.responseSource = responseSource;
	}

	
}
