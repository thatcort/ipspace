/*
 * Created on 7-Jan-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.lang.TextBuilder;

import com.liminal.ipspace.IPSpace;
import com.liminal.ipspace.data.Registry;
import com.liminal.ipspace.whois.WhoisManager.Source;

public class WhoisEngine {

	private static Logger LOG = Logger.getLogger(WhoisEngine.class.getName());
	
	private WhoisDB db;
	
	private HashMap<Registry, InetAddress> serverToAddressMap = new HashMap<Registry, InetAddress>();
	
	public WhoisEngine(WhoisDB db) {
		this.db = db;
	}
	
	public void shutdown() {
		try {
			db.shutdown();
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Problem shutting down database.", e);
		}
	}

//	/** @deprecated */
//	public NetRequestResult processRequest(Request request, Set<Source> sources) throws SQLException, UnknownHostException, IOException {
//		
//		String reqStr = request.getRequest() + "\n";
//		
//		// first search in the database:
//		String response = null;
//		Source source = null;
//		
//		if (sources.contains(Source.LOCAL)) {
//			Reader reader = db.getMostRecentResponse(request);
//			response = readResponse(reader, "local");
//			source = Source.LOCAL;
//		}
//		
//		if (response == null && sources.contains(Source.REMOTE)) {
//			LOG.info("making whois request: " + request.getServer() + ": " + request.getRequest());
//
//			InetAddress address = (InetAddress) serverToAddressMap.get(request.getServer());
//			if (address == null) {
//				address = InetAddress.getByName(request.getServer());
//				serverToAddressMap.put(request.getServer(), address);
//			}
//
//			Socket socket = new Socket(address, 43);
//			OutputStream out = socket.getOutputStream();
//			out.write(reqStr.getBytes());
//			InputStream in = socket.getInputStream();
//			BufferedReader buf = new BufferedReader(new InputStreamReader(in));
//			source = Source.REMOTE;
//
//			// check for a socket timeout
//			buf.mark(1024);
//			String line = buf.readLine();
//			if (line.indexOf("Timeout") >= 0)
//				throw new IOException("Connection to " + request.getServer() + " timed out.");
//			buf.reset();
//			
//			
//			response = readResponse(buf, request.getServer());
//			
//			db.logSearch(request, response);
//			
////			// log the request/response to the db
////			db.logSearch(request, response);
////			response = db.getMostRecentResponse(request);
//		}
//		
//		
//		return new NetRequestResult(request, response, source);
//	}
	
	public NetRequestResult processRequest(Request request, Source source) throws SQLException, UnknownHostException, IOException {
		if (source == Source.LOCAL)
			return processLocalRequest(request);
		else
			return processRemoteRequest(request);
	}
	
	private NetRequestResult processLocalRequest(Request request) throws SQLException, UnknownHostException, IOException {
		Reader reader = db.getMostRecentResponse(request);
		String response = readResponse(reader);
		
		return new NetRequestResult(request, response, Source.LOCAL);
	}
	
	private NetRequestResult processRemoteRequest(Request request) throws SQLException, UnknownHostException, IOException {
		String reqStr = request.getRequest() + "\n";
		
		// first search in the database:
		LOG.info("making whois request: " + request.getRegistry() + ": " + request.getRequest());

		Socket socket = null;
		String response = null;
		Registry registry = request.getRegistry();
		
		try {
			InetAddress address = (InetAddress) serverToAddressMap.get(request.getRegistry());
			if (address == null) {
				address = InetAddress.getByName(registry.getWhoisServer());
				serverToAddressMap.put(registry, address);
			}
	
			socket = new Socket(address, 43);
			int timeout = IPSpace.getConnectionTimeout();
			socket.setSoTimeout(timeout);
			OutputStream out = socket.getOutputStream();
			out.write(reqStr.getBytes());
			InputStream in = socket.getInputStream();
			BufferedReader buf = new BufferedReader(new InputStreamReader(in));
	
			// check for a socket timeout
			buf.mark(1024);
			String line = buf.readLine();
			if (line.indexOf("Timeout") >= 0)
				throw new IOException("Connection to " + registry + " timed out.");
			buf.reset();
			
			
			response = readResponse(buf);
			
			db.logSearch(request, response);
			
	//		// log the request/response to the db
	//		db.logSearch(request, response);
	//		response = db.getMostRecentResponse(request);
		} finally {
			if (socket != null)
				socket.close();
		}
		return new NetRequestResult(request, response, Source.REMOTE);
	}
	
	private String readResponse(Reader reader) throws IOException {
		if (reader == null)
			return null;
		
//		long t = System.currentTimeMillis();
		TextBuilder text = new TextBuilder();
		int c;
//		int count = 0;
//		int total = 0;
		while ((c = reader.read()) != -1) {
			text.append((char)c);
//			count++;
//			if (count > 100000) {
//				total += count;
//				count = 0;
//				long dt = System.currentTimeMillis() - t;
//				LOG.fine("Request " + request + ": Received " + total + " bytes; " + (dt > 0 ? (total * 1000 / dt) + " b/s": ""));
//			}
		}
		return text.toString();
	}
}
