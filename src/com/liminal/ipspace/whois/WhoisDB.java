/*
 * Created on 12-Oct-06
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import java.io.Reader;
import java.sql.SQLException;

/** @deprecated */
public interface WhoisDB {

	public abstract void shutdown() throws SQLException;

	/**
	 * @param server The server the request was made to
	 * @param objid The id of the object the request was made about (specific to a given server) 
	 * @param flags Flags included in the server request
	 * @param request The actual complete request made
	 * @param response The complete response from the server
	 */
	public abstract void logSearch(Request request, String response)
			throws SQLException;

	public abstract Reader getMostRecentResponse(Request request)
			throws SQLException;

}