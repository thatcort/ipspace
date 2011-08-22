/*
 * Created on 12-Oct-06
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import java.io.Reader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.liminal.ipspace.IPSpace;

/** @deprecated */
public class SingleConnectionWhoisDB /* implements WhoisDB */ {

//	private Connection connection;
//	
//	private static Logger LOG = Logger.getLogger(PooledWhoisDB.class.getName());
//	
//	private final boolean allowWrites;
//	
//	
//	public SingleConnectionWhoisDB() throws ClassNotFoundException, SQLException {
//		
//		String driverParam = IPSpace.getCommandLineArgValue("driver");
//		if (driverParam == null) driverParam = "org.hsqldb.jdbcDriver";
//		
//		Class.forName(driverParam);
//
//		String dbParam = IPSpace.getCommandLineArgValue("db");
//		if (dbParam == null) dbParam = "hsqldb:file:data/db/whois";
//
//		// Load the HSQL Database Engine JDBC driver
//		connection = DriverManager.getConnection("jdbc:" + dbParam, "sa", "");
//		
//		if (!testDBExists())
//			createDB();
//		
//		allowWrites = IPSpace.VALID_SOURCES.contains(WhoisManager.Source.REMOTE);
//		
//	}
//	
//	
//	private boolean testDBExists() throws SQLException {
//		boolean exists = false;
//		ResultSet rs = null;
//		try {
//			DatabaseMetaData md = connection.getMetaData();
//			rs = md.getTables(null, null, null, new String[] {"TABLE"});
//			int numFound = 0;
//			while (rs.next()) {
//				numFound++;
//			}
//			exists = numFound >= 3;
//		} finally {
//			try { rs.close(); } catch (Throwable t) {}
//		}
//		return exists;
//	}
//	
//	private void createDB() throws SQLException {
//		Statement st = null;
//		// create request table
//		String exp = "CREATE CACHED TABLE requests ( " +
//				"id INTEGER NOT NULL IDENTITY, " +
//				"server VARCHAR, " +
//				"objid VARCHAR, " +
//				"flags VARCHAR, " +
//				"type INTEGER, " +
//				"request VARCHAR)";
//		st = connection.createStatement();    // statements
//		st.executeUpdate(exp);    // run the query
//		st.close();
//		
//		// create response table
//		exp = "CREATE CACHED TABLE responses (" +
//				"id INTEGER NOT NULL IDENTITY, " +
//				"requestid INTEGER, " +
//				"tstamp TIMESTAMP, " +
//				"FOREIGN KEY (requestid) REFERENCES requests (id))";
//		st = connection.createStatement();    // statements
//		st.executeUpdate(exp);    // run the query
//		st.close();
//		
//		// create response text table
//		exp = "CREATE CACHED TABLE responseTexts (" +
//				"responseid INTEGER," +
//				"text LONGVARCHAR, " +
//				"FOREIGN KEY (responseId) REFERENCES responses (id))";
//		st = connection.createStatement();
//		st.executeUpdate(exp);
//		st.close();
//	}
//	
//	public synchronized void shutdown() throws SQLException {
//		Statement st = null;
//		try {
//			st = connection.createStatement();
//			st.execute("SHUTDOWN");// COMPACT");
//		} finally {
//			try { st.close(); } catch (Throwable t) {}
//		}
//	}
//
////	public void logSearch(Request request, String response) throws SQLException {
////		if (response == null) {
////			LOG.fine("Not logging null response for request: " + request);
////			return;
////		}
////		logSearch(request, new StringReader(response));
////	}
//	
//	/**
//	 * @param server The server the request was made to
//	 * @param objid The id of the object the request was made about (specific to a given server) 
//	 * @param flags Flags included in the server request
//	 * @param request The actual complete request made
//	 * @param response The complete response from the server
//	 */
//	public synchronized void logSearch(Request request, String response) throws SQLException {
//		if (response == null) {
//			LOG.fine("Not logging null response for request: " + request);
//			return;
//		}
//		
//		if (!allowWrites) {
//			throw new IllegalStateException("Attempt to write to db when not allows by settings.");
//		}
//		
//		LOG.fine("Storing result in db for request: " + request);
//		
//		String server = escapeString(request.getServer());
//		String objid = escapeString(request.getObjid());
//		String flags = escapeString(request.getFlags());
//		String reqStr = escapeString(request.getRequest());
//		
//		// find an existing request matching these parameters
//		String exp = "SELECT id FROM requests WHERE server="+server+" AND objid="+objid+" AND type="+request.getType();
//		Statement st = connection.createStatement();
//		ResultSet rs = st.executeQuery(exp);
//		boolean foundRequest = false;
//		int requestid = -1;
//		if (rs.next()) {
//			foundRequest = true;
//			requestid = rs.getInt(1);
//		}
//		st.close();
//		rs.close();
//
//		// now do the update
//		if (!foundRequest) {
//			String createRequestExp = "INSERT INTO requests (id, server, objid, flags, type, request) " +
//					"VALUES (NULL, " + server + ", " + objid + ", " + flags + ", " + request.getType() + ", " + reqStr + ")";
//			Statement reqSt = connection.createStatement();
//			int rval = reqSt.executeUpdate(createRequestExp);
//			if (rval != 1)
//				throw new SQLException("Update statement returned 0, indicating it did not create any rows.");
//			reqSt.close();
//			
//			// discover the created row id:
//			String idexp = "CALL IDENTITY()";
//			Statement idst = connection.createStatement();
//			ResultSet idrs = idst.executeQuery(idexp);
//			idrs.next();
//			requestid = idrs.getInt(1);
//			idrs.close();
//		}
//		
//		try {
//			connection.setAutoCommit(false);
//			
//			// add the response: id, requestid, tstamp
//			String createResponseExp = "INSERT INTO responses (id, requestid, tstamp) VALUES (NULL, " + requestid + ", CURRENT_TIMESTAMP )";
//			Statement resst = connection.createStatement();
//			int rval = resst.executeUpdate(createResponseExp);
//			if (rval != 1)
//				throw new SQLException("Update statement returned 0, indicating it did not create any rows.");
//			resst.close();
//			
//			// add the response text: responseid, text
//			String createResponseTextExp = "INSERT INTO responseTexts (responseid, text) VALUES ( identity() , ? )";
//			PreparedStatement restst = connection.prepareStatement(createResponseTextExp);
////				restst.setCharacterStream(1, response, Integer.MAX_VALUE);
//			restst.setString(1, response);
//			rval = restst.executeUpdate();
//			if (rval != 1)
//				throw new SQLException("Update statement returned 0, indicating it did not create any rows.");
//			restst.close();
//			
//			connection.commit();
//			
//		} catch (SQLException sex) {
//			LOG.log(Level.WARNING, "Problem writing result for request: " + request + ". Rolling back.", sex);
//			connection.rollback();
//			throw sex;
//		} catch (RuntimeException rex) {
//			LOG.log(Level.WARNING, "Problem writing result for request: " + request + ". Rolling back.", rex);
//			connection.rollback();
//			throw rex;
//		} catch (Error err) {
//			LOG.log(Level.WARNING, "Problem writing result for request: " + request + ". Rolling back.", err);
//			connection.rollback();
//			throw err;				
//		} finally {
//			connection.setAutoCommit(true);
//		}
//	}
//
//	public synchronized Reader getMostRecentResponse(Request request) throws SQLException {
//		String server = escapeString(request.getServer());
//		String objid = escapeString(request.getObjid());
//		
//		String exp = "SELECT text FROM responseTexts WHERE responseTexts.responseid = " +
//				"(SELECT TOP 1 responses.id FROM requests JOIN responses ON requests.id = responses.requestid " +
//				"WHERE requests.server="+server+" AND requests.objid="+objid+" AND requests.type="+request.getType() +
//				" ORDER BY responses.tstamp DESC)";
//		Statement st = connection.createStatement();
//		ResultSet rs = st.executeQuery(exp);
//		Reader reader = null;
//		if (rs.next())
//			reader = rs.getClob(1).getCharacterStream();
//		st.close();
//		rs.close();
//		return reader;
//	}
//	
//	private String escapeString(String s) {
//		return '\'' + s.trim().replaceAll("'", "''") + '\'';
//	}
//	
}
