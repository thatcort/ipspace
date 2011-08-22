/*
 * Created on 31-Mar-07
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastTable;

import com.liminal.ipspace.IPSpace;
import com.liminal.ipspace.data.Address;
import com.liminal.ipspace.data.AddressRange;
import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.data.NetTree;
import com.liminal.ipspace.data.Registry;
import com.liminal.ipspace.whois.WhoisManager.Source;

public class WhoisDB2H2 implements WhoisDB2 {

	private Connection connection;
	
	private static Logger LOG = Logger.getLogger(WhoisDB2H2.class.getName());
	
	private final boolean allowWrites;
	
	
	private PreparedStatement getInternetRootStmt;
	private PreparedStatement getMostRecentNetStmt;
	private PreparedStatement getNetInfoStmt;
	private PreparedStatement getMostRecentResponseStmt; // returns up to 1 responses.id
	private PreparedStatement getNetsByResponseStmt;
	private PreparedStatement getNetsByParentStmt;
	
	private PreparedStatement findRequestStmt;
	private PreparedStatement findNetInfoStmt;
	private PreparedStatement createRequestStmt;
	private PreparedStatement callIdentityStmt;
	private PreparedStatement createResponseStmt;
	private PreparedStatement createNetStmt;
	private PreparedStatement createInfoStmt;


	
	public WhoisDB2H2() throws ClassNotFoundException, SQLException {
		
		String driverParam = IPSpace.getCommandLineArgValue("driver");
		if (driverParam == null) driverParam = "org.h2.Driver";
		
		Class.forName(driverParam);

		String dbParam = IPSpace.getCommandLineArgValue("db");
		if (dbParam == null) dbParam = "h2:file:data/db2h2/whois";

		// Load the HSQL Database Engine JDBC driver
		connection = DriverManager.getConnection("jdbc:" + dbParam, "sa", "");
		
		if (!testDBExists())
			createDB();
		
		allowWrites = IPSpace.VALID_SOURCES.contains(WhoisManager.Source.REMOTE);

		String netFields = "nets.id, nets.parentid, nets.registry, nets.name, nets.org, nets.netregid, nets.rangestart, nets.rangeend ";
		
		String getInternetStr = "SELECT TOP 1 " + netFields + 
								"FROM nets JOIN responses ON nets.responseid = responses.id " + 
								"WHERE nets.parentid IS NULL AND nets.netregid = ? ORDER BY responses.tstamp DESC";
		getInternetRootStmt = connection.prepareStatement(getInternetStr);
		
		String getNetStr = "SELECT TOP 1 " + netFields +
							"FROM nets JOIN responses ON nets.responseid = responses.id " +
							"WHERE nets.parentid = ? AND nets.netregid = ? " +
							"ORDER BY responses.tstamp DESC";
		getMostRecentNetStmt = connection.prepareStatement(getNetStr);
		
		String getInfoStr = "SELECT info FROM netinfos JOIN nets ON nets.id = netinfos.netid WHERE nets.id = ?";
		getNetInfoStmt = connection.prepareStatement(getInfoStr);
		
		String gmr = "SELECT TOP 1 responses.id FROM requests JOIN responses ON requests.id = responses.requestid " +
						"WHERE requests.server= ? AND requests.objid= ? AND requests.type= ? "+
						" ORDER BY responses.tstamp DESC";
		getMostRecentResponseStmt = connection.prepareStatement(gmr);

		String getSubnetsByResponseStr = "SELECT " + netFields + " FROM nets WHERE nets.responseid = ?";
		getNetsByResponseStmt = connection.prepareStatement(getSubnetsByResponseStr);
	
		String getSubnetsByParentIdStr = "SELECT " + netFields + " FROM nets WHERE parentid = ? AND responseid = ?";
		getNetsByParentStmt = connection.prepareStatement(getSubnetsByParentIdStr);
		
		String findReqStr = "SELECT id FROM requests WHERE server= ? AND objid= ? AND type= ?";
		findRequestStmt = connection.prepareStatement(findReqStr);
		
		String findNetInfoStr = "SELECT id FROM netinfos WHERE netid = ?";
		findNetInfoStmt = connection.prepareStatement(findNetInfoStr);
		
		String createRequestExp = "INSERT INTO requests (server, objid, flags, type, request) VALUES ( ? , ? , ? , ? , ? )";
		createRequestStmt = connection.prepareStatement(createRequestExp);
		
		String idexp = "CALL IDENTITY()";
		callIdentityStmt = connection.prepareStatement(idexp);
		
		String createResponseExp = "INSERT INTO responses (requestid, tstamp) VALUES (? , CURRENT_TIMESTAMP )";
		createResponseStmt = connection.prepareStatement(createResponseExp);
		
		String createNetExp = "INSERT INTO nets (parentid, responseid, registry, name, org, netregid, rangestart, rangeend) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
		createNetStmt = connection.prepareStatement(createNetExp);
		
		String createInfoExp = "INSERT INTO netinfos (netid, responseid, info) VALUES (?, ?, ?)";
		createInfoStmt = connection.prepareStatement(createInfoExp);

//
//		String getNetStr = "SELECT id, parentid, registry, name, org, netregid, rangestart, rangeend FROM nets " +
//							"WHERE nets.responseid = (" + gmr + ")";
//		getMostRecentNetsStmt = connection.prepareStatement(getNetStr);
//		
////		String createResponseTextExp = "INSERT INTO responseTexts (responseid, text) VALUES ( identity() , ? )";
////		createResponseTextStmt = connection.prepareStatement(createResponseTextExp);
	}
	
	
	private boolean testDBExists() throws SQLException {
		boolean exists = false;
		ResultSet rs = null;
		try {
			DatabaseMetaData md = connection.getMetaData();
			rs = md.getTables(null, null, null, new String[] {"TABLE"});
			int numFound = 0;
			while (rs.next()) {
				numFound++;
			}
			exists = numFound >= 3;
		} finally {
			try { rs.close(); } catch (Throwable t) {}
		}
		return exists;
	}
	
	private void createDB() throws SQLException {
		Statement st = null;
		// create request table
		String exp = "CREATE CACHED TABLE requests ( " +
				"id BIGINT NOT NULL IDENTITY PRIMARY KEY, " +
				"server VARCHAR, " +
				"objid VARCHAR, " +
				"flags VARCHAR, " +
				"type INTEGER, " +
				"request VARCHAR)";
		st = connection.createStatement();    // statements
		st.executeUpdate(exp);    // run the query
		st.close();
		
		exp = "CREATE INDEX ON requests (server, objid, type)";
		st = connection.createStatement();
		st.executeUpdate(exp);
		st.close();
		
		// create response table
		exp = "CREATE CACHED TABLE responses (" +
				"id BIGINT NOT NULL IDENTITY PRIMARY KEY, " +
				"requestid BIGINT, " +
				"tstamp TIMESTAMP, " +
				"FOREIGN KEY (requestid) REFERENCES requests (id))";
		st = connection.createStatement();    // statements
		st.executeUpdate(exp);    // run the query
		st.close();

		exp = "CREATE CACHED TABLE nets ( " +
				"id BIGINT NOT NULL IDENTITY PRIMARY KEY, " +
				"parentid BIGINT, " +
				"responseid BIGINT, " +
				"registry VARCHAR, " +
				"name VARCHAR, " +
				"org VARCHAR, " +
				"netregid VARCHAR, " +
				"rangestart BIGINT, " +
				"rangeend BIGINT, " +
				"FOREIGN KEY (parentid) REFERENCES nets (id), " +
				"FOREIGN KEY (responseid) REFERENCES responses (id))";
		st = connection.createStatement();    // statements
		st.executeUpdate(exp);    // run the query
		st.close();

		exp = "CREATE INDEX ON nets (parentid)";
		st = connection.createStatement();
		st.executeUpdate(exp);
		st.close();
		
		exp = "CREATE CACHED TABLE netinfos ( " +
				"id BIGINT NOT NULL IDENTITY PRIMARY KEY, " +
				"netid BIGINT, " +
				"responseid BIGINT, " +
				"info VARCHAR, " +
				"FOREIGN KEY (netid) REFERENCES nets (id), " +
				"FOREIGN KEY (responseid) REFERENCES responses (id))";
		st = connection.createStatement();    // statements
		st.executeUpdate(exp);    // run the query
		st.close();
		
		exp = "CREATE INDEX ON netinfos (netid)";
		st = connection.createStatement();
		st.executeUpdate(exp);
		st.close();
		
//		exp = "CREATE CACHED TABLE hassubnets ( " +
//				"hassubs BOOLEAN, " +
//				"FOREIGN KEY (netid) REFERENCES nets (id))";
//		st = connection.createStatement();    // statements
//		st.executeUpdate(exp);    // run the query
//		st.close();
		
//		// create response text table
//		exp = "CREATE CACHED TABLE responseTexts (" +
//				"responseid INTEGER," +
//				"text CLOB, " +
//				"FOREIGN KEY (responseId) REFERENCES responses (id))";
//		st = connection.createStatement();
//		st.executeUpdate(exp);
//		st.close();
		
	}
	
	public void shutdown() throws SQLException {
		Statement st = null;
		try {
			st = connection.createStatement();
			st.execute("SHUTDOWN");// COMPACT");
		} finally {
			try { st.close(); } catch (Throwable t) {}
		}
	}


	public synchronized GetNetResult getNet(Net parent, String id) throws SQLException {
//		// if the parent has a dbid then we can use that to search for subnets
//		// otherwise we need to get the nets based on the most recent matching request
//		Long parentid = parent.getDBID();
//		if (parentid == null) {
//			Long responseid = getMostRecentResponseId(parent, Request.NET_TYPE);
//			if (responseid == null)
//				return new GetNetResult(null, Source.LOCAL);
//			
//			Net[] nets = getNetsByResponseId(parent, responseid);
//			if (nets.length > 1)
//				throw new IllegalStateException("Found more than one net for given id. Parent: " + parent + "; id=" + id);
//			if (nets.length == 0)
//				return new GetNetResult(null, Source.LOCAL);
//			
//			return new GetNetResult(nets[0], Source.LOCAL);
//		}
		
		
		Long parentid = (parent == null ? null : parent.getDBID());
		
		ResultSet rs = null;
		if (parentid == null) {
			getInternetRootStmt.setString(1, id);
			rs = getInternetRootStmt.executeQuery();
		} else {
			getMostRecentNetStmt.setLong(1, parentid);
			getMostRecentNetStmt.setString(2, id);
			rs = getMostRecentNetStmt.executeQuery();
		}
		
		Net net = null;
		if (rs.next())
			net = createNet(parent, rs);
		rs.close();
		return new GetNetResult(net, Source.LOCAL);
	}
	
	
	public synchronized GetSubnetResult getSubNets(Net parent) throws SQLException {
		Long parentid = parent.getDBID();
		
		// if the parent's id is null (e.g. was created from an rir report) then we need to look up its children by finding the appropriate request
		Net[] nets = null;
//		if (parentid == null) {
			Long responseid = getMostRecentResponseId(parent, Request.SUBNET_TYPE);
			if (responseid != null) {
				nets = getNetsByParentId(parent, responseid);
//				nets = getNetsByResponseId(parent, responseid);
			}
//		} else { // otherwise we can use the parent id to find all nets that reference that one as its parent
//WORKING!!! THIS WON'T WORK, SINCE WE CAN'T DISTINGUISH BETWEEN A NET WITHOUT ANY CHILDREN AND A NET WE HAVEN'T MADE A SUBNET CALL FOR YET!!!
//			nets = getNetsByParentId(parent);
//		}

		return new GetSubnetResult(parent, nets, Source.LOCAL);
	}


	public synchronized GetNetInfoResult getNetInfo(Net net) throws SQLException {
		String info = null;
		if (net.getDBID() != null) {
			getNetInfoStmt.setLong(1, net.getDBID());
			
			ResultSet rs = getNetInfoStmt.executeQuery();
			if (rs.next())
				info = rs.getString(1);
			rs.close();
		}
		
		return new GetNetInfoResult(net, info, Source.LOCAL);
	}


	private Long getMostRecentResponseId(Net net, int type) throws SQLException {
		Registry registry = net.getRegistry();
		String server = registry.getWhoisServer();
		
		getMostRecentResponseStmt.setString(1, server);
		getMostRecentResponseStmt.setString(2, net.getId());
		getMostRecentResponseStmt.setInt(3, type);
		
		ResultSet rs = getMostRecentResponseStmt.executeQuery();
		boolean found = false;
		long responseid = 0;
		if (rs.next()) {
			found = true;
			responseid = rs.getLong(1);
		}
		rs.close();
		
		return found ? responseid : null;
	}
	
	private Net[] getNetsByResponseId(Net parent, long responseid) throws SQLException {
		getNetsByResponseStmt.setLong(1, responseid);
		return createNets(parent, getNetsByResponseStmt);
	}
	
	private Net[] getNetsByParentId(Net parent, long responseid) throws SQLException {
if (parent.getDBID() == null)
	return new Net[0]; // FIXME: This is a HACK to prevent NullPtrs!!!!!!!!!!!!!
		getNetsByParentStmt.setLong(1, parent.getDBID());
		getNetsByParentStmt.setLong(2, responseid);
		return createNets(parent, getNetsByParentStmt);
	}
	
	private Net[] createNets(Net parent, PreparedStatement stmt) throws SQLException {
		ResultSet rs = stmt.executeQuery();
		FastTable<Net> nets = new FastTable<Net>();
		while (rs.next()) {
			Net net = createNet(parent, rs);
			nets.add(net);
		}
		rs.close();
		
		return nets.toArray(new Net[nets.size()]);
	}
	
	private Net createNet(Net parent, ResultSet rs) throws SQLException {
		long id = rs.getLong(1);
//		long parentid = rs.getLong(2);
		String regName = rs.getString(3);
		String name = rs.getString(4);
		String org = rs.getString(5);
		String regid = rs.getString(6);
		long rangeStart = rs.getLong(7);
		long rangeEnd = rs.getLong(8);
		
		AddressRange range = new AddressRange(new Address(rangeStart), new Address(rangeEnd));
		Registry registry = Registry.getRegistryByName(regName);
		
		Net net = new Net(parent, range, name, registry, regid, org);
		net.setDBID(id);
		
		return net;
	}
	
	

	public synchronized void logResult(FullRequestResult result) throws SQLException {
		Request request = result.getRequest();
		
		long requestid = logRequest(request);
		
		try {
			connection.setAutoCommit(false);

			long responseid = createResponse(requestid);
			
			NetTree nettree = result.getNetTree();
			boolean fillsGaps = nettree.getFillGaps();
			nettree.setFillGaps(false);
			logNetTree(nettree, nettree.getRoot(), responseid);
			nettree.setFillGaps(fillsGaps);
			
			NetInfo[] infos = result.getNetInfos();
			for (NetInfo info : infos) {
				logNetInfo(info, responseid);
			}
			
			connection.commit();
		} catch (SQLException sex) {
			LOG.log(Level.WARNING, "Problem writing result for request: " + request + ". Rolling back.", sex);
			connection.rollback();
			throw sex;
		} catch (RuntimeException rex) {
			LOG.log(Level.WARNING, "Problem writing result for request: " + request + ". Rolling back.", rex);
			connection.rollback();
			throw rex;
		} catch (Error err) {
			LOG.log(Level.WARNING, "Problem writing result for request: " + request + ". Rolling back.", err);
			connection.rollback();
			throw err;				
		} finally {
			connection.setAutoCommit(true);
		}
	}
	
	private void logNetTree(NetTree nettree, Net current, long responseid) throws SQLException {
		logNet(current, null, responseid);
		
		Net[] subnets = nettree.getSubnets(current);
		if (subnets != null) {
			for (Net net : subnets) {
				logNetTree(nettree, net, responseid);
			}
		}
	}
	

	public synchronized void logResult(Request request, GetNetResult result) throws SQLException {
		long requestid = logRequest(request);
		
		try {
			connection.setAutoCommit(false);

			long responseid = createResponse(requestid);
			
			logNet(result.getNet(), result.getInfo(), responseid);
			
			connection.commit();
		} catch (SQLException sex) {
			LOG.log(Level.WARNING, "Problem writing result for request: " + request + ". Rolling back.", sex);
			connection.rollback();
			throw sex;
		} catch (RuntimeException rex) {
			LOG.log(Level.WARNING, "Problem writing result for request: " + request + ". Rolling back.", rex);
			connection.rollback();
			throw rex;
		} catch (Error err) {
			LOG.log(Level.WARNING, "Problem writing result for request: " + request + ". Rolling back.", err);
			connection.rollback();
			throw err;				
		} finally {
			connection.setAutoCommit(true);
		}
	}


	public synchronized void logResult(Request request, GetSubnetResult result) throws SQLException {
		long requestid = logRequest(request);
		
		try {
			connection.setAutoCommit(false);

			long responseid = createResponse(requestid);
			
			logNet(result.getNet(), result.getInfo(), responseid); // log the parent in case it has more info or was never logged
			
			Net[] subnets = result.getSubnets();
			String[] subinfos = result.getSubnetInfos();
			for (int i = 0; i < subnets.length; i++) {
				logNet(subnets[i], subinfos == null ? null : subinfos[i], responseid);				
			}
			
			connection.commit();
		} catch (SQLException sex) {
			LOG.log(Level.WARNING, "Problem writing result for request: " + request + ". Rolling back.", sex);
			connection.rollback();
			throw sex;
		} catch (RuntimeException rex) {
			LOG.log(Level.WARNING, "Problem writing result for request: " + request + ". Rolling back.", rex);
			connection.rollback();
			throw rex;
		} catch (Error err) {
			LOG.log(Level.WARNING, "Problem writing result for request: " + request + ". Rolling back.", err);
			connection.rollback();
			throw err;				
		} finally {
			connection.setAutoCommit(true);
		}
	}


	public void logResult(Request request, GetNetInfoResult result) throws SQLException {
		long requestid = logRequest(request);
		
		try {
			connection.setAutoCommit(false);

			long responseid = createResponse(requestid);
			
			logNetInfo(result.getNet(), result.getInfo(), responseid);
			
			connection.commit();
		} catch (SQLException sex) {
			LOG.log(Level.WARNING, "Problem writing result for request: " + request + ". Rolling back.", sex);
			connection.rollback();
			throw sex;
		} catch (RuntimeException rex) {
			LOG.log(Level.WARNING, "Problem writing result for request: " + request + ". Rolling back.", rex);
			connection.rollback();
			throw rex;
		} catch (Error err) {
			LOG.log(Level.WARNING, "Problem writing result for request: " + request + ". Rolling back.", err);
			connection.rollback();
			throw err;				
		} finally {
			connection.setAutoCommit(true);
		}
	}
	
	private synchronized long logRequest(Request request) throws SQLException {
		Registry registry = request.getRegistry();
		String server = registry.getWhoisServer();
		String objid = request.getObjid();
		String flags = request.getFlags();
		String reqStr = request.getRequest();
		
		findRequestStmt.setString(1, server);
		findRequestStmt.setString(2, objid);
		findRequestStmt.setInt(3, request.getType());
		ResultSet rs = findRequestStmt.executeQuery();
		boolean foundRequest = false;
		long requestid = -1;
		if (rs.next()) {
			foundRequest = true;
			requestid = rs.getLong(1);
		}
		rs.close();

		// now do the update
		if (!foundRequest) {
			createRequestStmt.setString(1, server);
			createRequestStmt.setString(2, objid);
			createRequestStmt.setString(3, flags);
			createRequestStmt.setInt(4, request.getType());
			createRequestStmt.setString(5, reqStr);
			int rval = createRequestStmt.executeUpdate();
			if (rval != 1)
				throw new SQLException("Update statement returned 0, indicating it did not create any rows.");
			
			// discover the created row id:
			requestid = callIdentity();
		}
		
		return requestid;
	}
	
	
	private synchronized long callIdentity() throws SQLException {
		// discover the created row id:
		ResultSet idrs = callIdentityStmt.executeQuery();
		idrs.next();
		long id = idrs.getLong(1);
		idrs.close();
		return id;
	}
	
	
	private synchronized long createResponse(long requestid) throws SQLException {
		// add the response: id, requestid, tstamp
		createResponseStmt.setLong(1, requestid);
		int rval = createResponseStmt.executeUpdate();
		if (rval != 1)
			throw new SQLException("Update statement returned 0, indicating it did not create any rows.");
		
		return callIdentity();
	}

	
	private synchronized void logNet(Net net, String info, long responseid) throws SQLException {
		Long netdbid = net.getDBID();
		if (netdbid == null) { // only add the net if it doesn't already exist
			Long pid = net.getParent() == null ? null : net.getParent().getDBID();
			if (pid == null)
				createNetStmt.setNull(1, Types.BIGINT);
			else
				createNetStmt.setLong(1, pid);
			
			createNetStmt.setLong(2, responseid);
			createNetStmt.setString(3, net.getRegistry().getName());
			createNetStmt.setString(4, net.getName());
			createNetStmt.setString(5, net.getOrg());
			createNetStmt.setString(6, net.getId());
			createNetStmt.setLong(7, net.getRange().min());
			createNetStmt.setLong(8, net.getRange().max());
			int rval = createNetStmt.executeUpdate();
			if (rval != 1)
				throw new SQLException("Create net statement returned 0, indicating it did not create a net.");
			
			// discover the created row id:
			netdbid = callIdentity();
			
			net.setDBID(netdbid);
		}
		
		if (info != null)
			logNetInfo(net, info, responseid);
	}
	
	private synchronized void logNetInfo(NetInfo ni, long responseid) throws SQLException {
		logNetInfo(ni.getNet(), ni.getInfo(), responseid);
	}
	
	private synchronized void logNetInfo(Net net, String info, long responseid) throws SQLException {
		// first check if one already exists
		findNetInfoStmt.setLong(1, net.getDBID());
		ResultSet rs = findNetInfoStmt.executeQuery();
		boolean alreadyLogged = false;
		if (rs.next()) {
			alreadyLogged = true;
		}
		rs.close();
		if (alreadyLogged)
			return;
		
		createInfoStmt.setLong(1, net.getDBID());
		createInfoStmt.setLong(2, responseid);
		createInfoStmt.setString(3, info);
		int rval = createInfoStmt.executeUpdate();
		if (rval != 1)
			throw new SQLException("Create netinfo statement returned 0, indicating it did not create the row.");
	}


}
