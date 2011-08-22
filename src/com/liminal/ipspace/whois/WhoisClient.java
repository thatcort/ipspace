/*
 * Created on 2-Jan-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.whois.WhoisManager.Source;

/** @deprecated */
public interface WhoisClient {

	String getServer();
	
	NetRequestResult getSubNets(Net net, Source source) throws Exception;
	
	NetRequestResult getNet(Net parent, String id, Source source) throws Exception;
	
	NetInfoRequestResult getNetInfo(Net net, Source source) throws Exception;
	
	
}
