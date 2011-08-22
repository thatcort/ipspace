/*
 * Created on 31-Mar-07
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import java.sql.SQLException;

import com.liminal.ipspace.data.Net;

public interface WhoisDB2 {

	void shutdown() throws SQLException;
	
	GetSubnetResult getSubNets(Net net) throws SQLException;
	GetNetResult getNet(Net parent, String id) throws SQLException;
	GetNetInfoResult getNetInfo(Net net) throws SQLException;
	
	void logResult(FullRequestResult result) throws SQLException;
	
	void logResult(Request request, GetNetResult result) throws SQLException;
	void logResult(Request request, GetSubnetResult result) throws SQLException;
	void logResult(Request request, GetNetInfoResult result) throws SQLException;
}
