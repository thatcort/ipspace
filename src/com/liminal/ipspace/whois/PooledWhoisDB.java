/*
 * Created on 6-Jan-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

//import java.io.Reader;
//import java.sql.Connection;
//import java.sql.DatabaseMetaData;
//import java.sql.DriverManager;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
//import org.apache.commons.dbcp.DriverManagerConnectionFactory;
//import org.apache.commons.dbcp.PoolableConnectionFactory;
//import org.apache.commons.dbcp.PoolingDriver;
//import org.apache.commons.pool.impl.GenericObjectPool;
//
//import com.liminal.ipspace.IPSpace;

public class PooledWhoisDB /*implements WhoisDB */{
//
////	private Connection connection;
//	
//	private static Logger LOG = Logger.getLogger(PooledWhoisDB.class.getName());
//	
//	private final boolean allowWrites;
//	
//	
//	public PooledWhoisDB() throws ClassNotFoundException, SQLException {
//		
//		Class.forName("org.hsqldb.jdbcDriver");
////		Class.forName("org.apache.commons.dbcp.PoolingDriver");
//		
//		String dbParam = IPSpace.getCommandLineArgValue("db");
//		if (dbParam == null) dbParam = "hsqldb:file:data/db/whois";
//		
//		// create a connection pool:
//		GenericObjectPool connectionPool = new GenericObjectPool(null);
//		DriverManagerConnectionFactory dmcf = new DriverManagerConnectionFactory("jdbc:" + dbParam, "sa", "");
//		PoolableConnectionFactory factory = new PoolableConnectionFactory(dmcf, connectionPool, null, null, false, true);
//		PoolingDriver driver = new PoolingDriver();
//		driver.registerPool("whoisdb", connectionPool);
//		
//		// Load the HSQL Database Engine JDBC driver
////		connection = createConnection();
//		
//		if (!testDBExists())
//			createDB();
//		
//		allowWrites = IPSpace.VALID_SOURCES.contains(WhoisManager.Source.REMOTE);
//		
//	}
//	
//	private Connection getConnection() throws SQLException {
//		return DriverManager.getConnection("jdbc:apache:commons:dbcp:whoisdb", "sa", "");		
//	}
//	
//	private void returnConnection(Connection conn) throws SQLException {
//		conn.close();
//	}
//	
//	
//	private boolean testDBExists() throws SQLException {
//		boolean exists = false;
//		Connection connection = null;
//		ResultSet rs = null;
//		try {
//			connection = getConnection();
//			DatabaseMetaData md = connection.getMetaData();
//			rs = md.getTables(null, null, null, new String[] {"TABLE"});
//			int numFound = 0;
//			while (rs.next()) {
//				numFound++;
//			}
//			exists = numFound >= 3;
//		} finally {
//			try { rs.close(); } catch (Throwable t) {}
//			try { returnConnection(connection); } catch (Throwable t) {}
//		}
//		return exists;
//	}
//	
//	private void createDB() throws SQLException {
//		Connection connection = null;
//		Statement st = null;
//		try {
//			connection = getConnection();
//			
//			// create request table
//			String exp = "CREATE CACHED TABLE requests ( " +
//					"id INTEGER NOT NULL IDENTITY, " +
//					"server VARCHAR, " +
//					"objid VARCHAR, " +
//					"flags VARCHAR, " +
//					"type INTEGER, " +
//					"request VARCHAR)";
//			st = connection.createStatement();    // statements
//			st.executeUpdate(exp);    // run the query
//			st.close();
//			
//			// create response table
//			exp = "CREATE CACHED TABLE responses (" +
//					"id INTEGER NOT NULL IDENTITY, " +
//					"requestid INTEGER, " +
//					"tstamp TIMESTAMP, " +
//					"FOREIGN KEY (requestid) REFERENCES requests (id))";
//			st = connection.createStatement();    // statements
//			st.executeUpdate(exp);    // run the query
//			st.close();
//			
//			// create response text table
//			exp = "CREATE CACHED TABLE responseTexts (" +
//					"responseid INTEGER," +
//					"text LONGVARCHAR, " +
//					"FOREIGN KEY (responseId) REFERENCES responses (id))";
//			st = connection.createStatement();
//			st.executeUpdate(exp);
//			st.close();
//		} finally {
//			try { returnConnection(connection); } catch (Throwable t) {}
//		}
//	}
//	
//	/* (non-Javadoc)
//	 * @see com.liminal.ipspace.whois.WhoisDB#shutdown()
//	 */
//	public void shutdown() throws SQLException {
//		Connection connection = null;
//		Statement st = null;
//		try {
//			connection = getConnection();
//
//			st = connection.createStatement();
//			st.execute("SHUTDOWN");// COMPACT");
//		} finally {
//			try { st.close(); } catch (Throwable t) {}
//			try { returnConnection(connection); } catch (Throwable t) {}
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
//	/* (non-Javadoc)
//	 * @see com.liminal.ipspace.whois.WhoisDB#logSearch(com.liminal.ipspace.whois.Request, java.lang.String)
//	 */
//	public void logSearch(Request request, String response) throws SQLException {
//		if (response == null) {
//			LOG.fine("Not logging null response for request: " + request);
//			return;
//		}
//		
//		if (!allowWrites) {
//			throw new IllegalStateException("Attempt to write to db when not allows by settings.");
//		}
//		
//		Connection connection = null;
//		try {
//			LOG.fine("Storing result in db for request: " + request);
//			
//			connection = getConnection();
//		
//			String server = escapeString(request.getServer());
//			String objid = escapeString(request.getObjid());
//			String flags = escapeString(request.getFlags());
//			String reqStr = escapeString(request.getRequest());
//			
//			// find an existing request matching these parameters
//			String exp = "SELECT id FROM requests WHERE server="+server+" AND objid="+objid+" AND type="+request.getType();
//			Statement st = connection.createStatement();
//			ResultSet rs = st.executeQuery(exp);
//			boolean foundRequest = false;
//			int requestid = -1;
//			if (rs.next()) {
//				foundRequest = true;
//				requestid = rs.getInt(1);
//			}
//			st.close();
//			rs.close();
//	
//			// now do the update
//			if (!foundRequest) {
//				String createRequestExp = "INSERT INTO requests (id, server, objid, flags, type, request) " +
//						"VALUES (NULL, " + server + ", " + objid + ", " + flags + ", " + request.getType() + ", " + reqStr + ")";
//				Statement reqSt = connection.createStatement();
//				int rval = reqSt.executeUpdate(createRequestExp);
//				if (rval != 1)
//					throw new SQLException("Update statement returned 0, indicating it did not create any rows.");
//				reqSt.close();
//				
//				// discover the created row id:
//				String idexp = "CALL IDENTITY()";
//				Statement idst = connection.createStatement();
//				ResultSet idrs = idst.executeQuery(idexp);
//				idrs.next();
//				requestid = idrs.getInt(1);
//				idrs.close();
//			}
//			
//			try {
//				connection.setAutoCommit(false);
//				
//				// add the response: id, requestid, tstamp
//				String createResponseExp = "INSERT INTO responses (id, requestid, tstamp) VALUES (NULL, " + requestid + ", CURRENT_TIMESTAMP )";
//				Statement resst = connection.createStatement();
//				int rval = resst.executeUpdate(createResponseExp);
//				if (rval != 1)
//					throw new SQLException("Update statement returned 0, indicating it did not create any rows.");
//				resst.close();
//				
//				// add the response text: responseid, text
//				String createResponseTextExp = "INSERT INTO responseTexts (responseid, text) VALUES ( identity() , ? )";
//				PreparedStatement restst = connection.prepareStatement(createResponseTextExp);
////				restst.setCharacterStream(1, response, Integer.MAX_VALUE);
//				restst.setString(1, response);
//				rval = restst.executeUpdate();
//				if (rval != 1)
//					throw new SQLException("Update statement returned 0, indicating it did not create any rows.");
//				restst.close();
//				
//				connection.commit();
//				
//			} catch (SQLException sex) {
//				LOG.log(Level.WARNING, "Problem writing result for request: " + request + ". Rolling back.", sex);
//				connection.rollback();
//				throw sex;
//			} catch (RuntimeException rex) {
//				LOG.log(Level.WARNING, "Problem writing result for request: " + request + ". Rolling back.", rex);
//				connection.rollback();
//				throw rex;
//			} catch (Error err) {
//				LOG.log(Level.WARNING, "Problem writing result for request: " + request + ". Rolling back.", err);
//				connection.rollback();
//				throw err;				
//			} finally {
//				connection.setAutoCommit(true);
//			}
//		} finally {
//			try { returnConnection(connection); } catch (Throwable t) {}
//		}
//	}
//
//	/* (non-Javadoc)
//	 * @see com.liminal.ipspace.whois.WhoisDB#getMostRecentResponse(com.liminal.ipspace.whois.Request)
//	 */
//	public Reader getMostRecentResponse(Request request) throws SQLException {
//		Connection connection = null;
//		try {
//			connection = getConnection();
//			String server = escapeString(request.getServer());
//			String objid = escapeString(request.getObjid());
//			
//			String exp = "SELECT text FROM responseTexts WHERE responseTexts.responseid = " +
//					"(SELECT TOP 1 responses.id FROM requests JOIN responses ON requests.id = responses.requestid " +
//					"WHERE requests.server="+server+" AND requests.objid="+objid+" AND requests.type="+request.getType() +
//					" ORDER BY responses.tstamp DESC)";
//			Statement st = connection.createStatement();
//			ResultSet rs = st.executeQuery(exp);
//			Reader reader = null;
//			if (rs.next())
//				reader = rs.getClob(1).getCharacterStream();
//			st.close();
//			rs.close();
//			return reader;
//		} finally {
//			try { returnConnection(connection); } catch (Throwable t) {}
//		}
//	}
//	
//	private String escapeString(String s) {
//		return '\'' + s.trim().replaceAll("'", "''") + '\'';
//	}
//	
}
