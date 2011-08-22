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
public class SingleConnectionPreparedStatementWhoisDB {
//implements WhoisDB {
//
//	private Connection connection;
//	
//	private static Logger LOG = Logger.getLogger(PooledWhoisDB.class.getName());
//	
//	private final boolean allowWrites;
//	
//	
//	private PreparedStatement getMostRecentStmt;
//	private PreparedStatement findRequestStmt;
//	private PreparedStatement createRequestStmt;
//	private PreparedStatement callIdentityStmt;
//	private PreparedStatement createResponseStmt;
//	private PreparedStatement createResponseTextStmt;
//	
//	
//	public SingleConnectionPreparedStatementWhoisDB() throws ClassNotFoundException, SQLException {
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
//		String gmr = "SELECT text FROM responseTexts WHERE responseTexts.responseid = " +
//					"(SELECT TOP 1 responses.id FROM requests JOIN responses ON requests.id = responses.requestid " +
//					"WHERE requests.server= ? AND requests.objid= ? AND requests.type= ? "+
//					" ORDER BY responses.tstamp DESC)";
//		getMostRecentStmt = connection.prepareStatement(gmr);
//
//		String findReqStr = "SELECT id FROM requests WHERE server= ? AND objid= ? AND type= ?";
//		findRequestStmt = connection.prepareStatement(findReqStr);
//		
//		String createRequestExp = "INSERT INTO requests (id, server, objid, flags, type, request) VALUES (NULL, ? , ? , ? , ? , ? )";
//		createRequestStmt = connection.prepareStatement(createRequestExp);
//		
//		String idexp = "CALL IDENTITY()";
//		callIdentityStmt = connection.prepareStatement(idexp);
//		
//		String createResponseExp = "INSERT INTO responses (id, requestid, tstamp) VALUES (NULL, ? , CURRENT_TIMESTAMP )";
//		createResponseStmt = connection.prepareStatement(createResponseExp);
//		
//		String createResponseTextExp = "INSERT INTO responseTexts (responseid, text) VALUES ( identity() , ? )";
//		createResponseTextStmt = connection.prepareStatement(createResponseTextExp);
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
//		
//		exp = "CREATE INDEX reqIndex ON requests (server, objid, type)";
//		st = connection.createStatement();
//		st.executeUpdate(exp);
//		st.close();
//		
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
//		findRequestStmt.setString(1, server);
//		findRequestStmt.setString(2, objid);
//		findRequestStmt.setInt(3, request.getType());
//		ResultSet rs = findRequestStmt.executeQuery();
//		boolean foundRequest = false;
//		int requestid = -1;
//		if (rs.next()) {
//			foundRequest = true;
//			requestid = rs.getInt(1);
//		}
//		rs.close();
//
//		// now do the update
//		if (!foundRequest) {
//			createRequestStmt.setString(1, server);
//			createRequestStmt.setString(2, objid);
//			createRequestStmt.setString(3, flags);
//			createRequestStmt.setInt(4, request.getType());
//			createRequestStmt.setString(5, reqStr);
//			int rval = createRequestStmt.executeUpdate();
//			if (rval != 1)
//				throw new SQLException("Update statement returned 0, indicating it did not create any rows.");
//			
//			// discover the created row id:
//			ResultSet idrs = callIdentityStmt.executeQuery();
//			idrs.next();
//			requestid = idrs.getInt(1);
//			idrs.close();
//		}
//		
//		try {
//			connection.setAutoCommit(false);
//			
//			// add the response: id, requestid, tstamp
//			createResponseStmt.setInt(1, requestid);
//			int rval = createResponseStmt.executeUpdate();
//			if (rval != 1)
//				throw new SQLException("Update statement returned 0, indicating it did not create any rows.");
//			
////			String responseStr = readResponse(response);
//			
//			// add the response text: responseid, text
////				restst.setCharacterStream(1, response, Integer.MAX_VALUE);
//			createResponseTextStmt.setString(1, response);
//			rval = createResponseTextStmt.executeUpdate();
//			if (rval != 1)
//				throw new SQLException("Update statement returned 0, indicating it did not create any rows.");
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
////		} catch (IOException iox) {
////			LOG.log(Level.WARNING, "Problem writing result for request: " + request + ". Rolling back.", iox);
////			connection.rollback();
////			throw new SQLException("Problem writing result for request: " + request + ". Rolling back. Due to: " + iox.getMessage());				
//		} finally {
//			connection.setAutoCommit(true);
//		}
//	}
//	
///*
//	private String readResponse(Reader reader) throws IOException {
//		if (reader == null)
//			return null;
//		
////		long t = System.currentTimeMillis();
//		TextBuilder text = new TextBuilder();
//		int c;
////		int count = 0;
////		int total = 0;
//		while ((c = reader.read()) != -1) {
//			text.append((char)c);
////			count++;
////			if (count > 100000) {
////				total += count;
////				count = 0;
////				long dt = System.currentTimeMillis() - t;
////				LOG.fine("Request " + request + ": Received " + total + " bytes; " + (dt > 0 ? (total * 1000 / dt) + " b/s": ""));
////			}
//		}
//		return text.toString();
//	}
//*/
//
//	public synchronized Reader getMostRecentResponse(Request request) throws SQLException {
//		String server = escapeString(request.getServer());
//		String objid = escapeString(request.getObjid());
//		
//		getMostRecentStmt.setString(1, server);
//		getMostRecentStmt.setString(2, objid);
//		getMostRecentStmt.setInt(3, request.getType());
//		ResultSet rs = getMostRecentStmt.executeQuery();
//		Reader reader = null;
//		if (rs.next())
//			reader = rs.getClob(1).getCharacterStream();
//		rs.close();
//		return reader;
//	}
//	
//	private String escapeString(String s) {
//		return s;
////		return '\'' + s.trim().replaceAll("'", "''") + '\'';
//	}
//
//
}
