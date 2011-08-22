/*
 * Created on 6-Apr-07
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
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Logger;

import com.liminal.ipspace.IPSpace;
import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.data.Registry;
import com.liminal.ipspace.whois.WhoisManager.Source;

public abstract class  WhoisClient2 {
	
	protected Registry registry;
	
	protected WhoisDB2 db;
	
	protected Logger LOG;
	
	public WhoisClient2(Registry registry, WhoisDB2 db) {
		this.registry = registry;
		this.db = db;
		
		LOG = Logger.getLogger(registry.getName() + "WhoisClient");
	}
	
	public String getServer() { return registry.getWhoisServer(); }
	
	
	public GetNetResult getNet(Net parent, String id, Source source) throws Exception {
		if (source == Source.LOCAL)
			return getNetLocally(parent, id);
		return getNetRemotely(parent, id);
	}

	public GetNetResult getNetLocally(Net parent, String id) throws Exception {
		return db.getNet(parent, id);		
	}
	
	public GetNetResult getNetRemotely(Net parent, String id) throws Exception {
		Request request = getNetRequest(parent, id);
		processRemoteRequest(request);
		return getNetLocally(parent, id);
	}

	
	
	public GetSubnetResult getSubnets(Net parent, Source source) throws Exception {
		if (source == Source.LOCAL)
			return getSubnetsLocally(parent);
		return getSubnetsRemotely(parent);
	}
	
	public GetSubnetResult getSubnetsLocally(Net parent) throws Exception {
		return db.getSubNets(parent);
	}
	
	public GetSubnetResult getSubnetsRemotely(Net parent) throws Exception {
		Request request = getSubnetsRequest(parent);
		processRemoteRequest(request);	
		return getSubnetsLocally(parent);
	}
	
	
	
	public GetNetInfoResult getNetInfo(Net net, Source source) throws Exception {
		if (source == Source.LOCAL)
			return getNetInfoLocally(net);
		return getNetInfoRemotely(net);
	}
	
	public GetNetInfoResult getNetInfoLocally(Net net) throws Exception {
		return db.getNetInfo(net);
	}
	
	public GetNetInfoResult getNetInfoRemotely(Net net) throws Exception {
		Request request = getNetInfoRequest(net);
		processRemoteRequest(request);
		return getNetInfoLocally(net);
	}

	
	
	protected void processRemoteRequest(Request request) throws Exception {
		BufferedReader reader = null;
		try {
			reader = getRemoteResponse(request);
			FullRequestResult fullResult = parseResponse(request, reader);
			if (request.getType() == Request.SUBNET_TYPE) {
				if (fullResult.getNetTree().getSubnets(request.getNet()) == null)
					fullResult.getNetTree().setNoChildren(request.getNet());
			}
			db.logResult(fullResult);
		} finally {
			if (reader != null)
				reader.close();
		}
	}
	
	
	
	protected abstract Request getNetRequest(Net parent, String id);
	protected abstract Request getSubnetsRequest(Net parent);
	protected abstract Request getNetInfoRequest(Net net);
	
	protected abstract FullRequestResult parseResponse(Request request, BufferedReader response) throws IOException;
	
	protected BufferedReader getRemoteResponse(Request request) throws IOException {
		String reqStr = request.getRequest() + "\n";
		
		// first search in the database:
		LOG.info("making whois request: " + request.getRegistry() + ": " + request.getRequest());

		Socket socket = null;
		Registry registry = request.getRegistry();
		
		InetAddress address = InetAddress.getByName(registry.getWhoisServer());

		socket = new Socket(address, 43);
		int timeout = IPSpace.getConnectionTimeout();
		socket.setSoTimeout(timeout);
		OutputStream out = socket.getOutputStream();
		out.write(reqStr.getBytes());
		InputStream in = socket.getInputStream();
		BufferedReader buf = new BufferedReader(new InputStreamReader(in));
	
		return buf;
//			// check for a socket timeout
//			buf.mark(1024);
//			String line = buf.readLine();
//			if (line.indexOf("Timeout") >= 0)
//				throw new IOException("Connection to " + registry + " timed out.");
//			buf.reset();
	}
	
	protected boolean checkLineForTimeout(String line) {
		boolean timeout = (line.indexOf("Timeout") >= 0);
		if (timeout)
			LOG.info("Timeout on message from " + registry.getName());
		return timeout;
	}
}
