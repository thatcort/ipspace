/*
 * Created on 7-Feb-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.lang.TextBuilder;
import javolution.util.FastList;

import com.liminal.ipspace.data.AddressRange;
import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.data.Registry;
import com.liminal.ipspace.whois.WhoisManager.Source;

/** This client should work for ripe, apnic, afrinic */
public class InternationalClient implements WhoisClient {

	protected WhoisManager whoisMgr;
	
	protected Registry registry;
	
	protected final Logger LOG;
	
	public InternationalClient(WhoisManager whoisMgr, Registry registry) {
		LOG = Logger.getLogger(getClass().getName() + ";registry=" + registry);
		this.whoisMgr = whoisMgr;
		this.registry = registry;
	}

	public String getServer() { return registry.getWhoisServer(); }
	
	public NetRequestResult getNet(Net parent, String id, Source source) throws Exception {
		NetRequestResult nrr = getNetRequestResult(id, source);
		parseNets(parent, nrr);
		return nrr;
	}

	public NetInfoRequestResult getNetInfo(Net net, Source source) throws Exception {
		// a bit complicated...
		// if the net's parent has the same registry as the net in question,
		// then first check a subnet query on the parent (we are more likely to have this result already)
		// if that doesn't work (possibly because the sources don't allow a net request)
		// then try a getNet query on the net itself
		NetRequestResult nrr = null;
		String info = null;
		if (net.getParent().getRegistry() == net.getRegistry()) {
			nrr = getSubnetRequestResult(net.getParent(), source);
			info = getNetInfo(net, nrr);
		}
		if (info == null) {
			nrr = getNetRequestResult(net, source);
			info = getNetInfo(net, nrr);
		}
		NetInfoRequestResult nirr = new NetInfoRequestResult(nrr.request, nrr.response, nrr.responseSource, info);
		return nirr;
	}

	public NetRequestResult getSubNets(Net net, Source source) throws Exception {
		NetRequestResult nrr = getSubnetRequestResult(net, source);
		parseNets(net, nrr);
		return nrr;
	}
	
	protected NetRequestResult getNetRequestResult(Net net, Source source) throws Exception {
		String id = net.getRange().getMinAddress() + " - " + net.getRange().getMaxAddress();
		return getNetRequestResult(id, source);
	}
	
	protected NetRequestResult getSubnetRequestResult(Net net, Source source) throws Exception {
		String flags = "-m -r ";
		AddressRange range = net.getRange();
		
//		// split requests that cross /8 boundaries:
//		int min8 = range.getMinAddress().getOctet(0);
//		int max8 = range.getMaxAddress().getOctet(0);
//		if (min8 != max8) {
//			// split the subnet request:
//			int numReqs = max8 - min8 + 1;
//			NetRequestResult[] nrrs = new NetRequestResult[numReqs];
//			for (int i=0; i < numReqs; i++) {
//				int oct = min8 + i;
//				String start = (oct == min8 ? range.getMinAddress().toString() : oct + ".0.0.0");
//				String end = (oct == max8 ? range.getMaxAddress().toString() : oct + "255.255.255");
//				String id0 = start + " - " + end;
//				nrrs[i] = getSubnetRequestResult(id0, source);
//			}
//			
//		}
		
		String query = flags + range.getMinAddress() + " - " + range.getMaxAddress();
		Request req = new Request(registry, flags, net.getId(), Request.SUBNET_TYPE, query);
		LOG.fine("Requesting: " + query);
		NetRequestResult nrr = whoisMgr.getWhoisEngine().processRequest(req, source);
		LOG.fine("Received response. source=" + nrr.responseSource + (nrr.response == null ? ", response=null" : ", length=" + nrr.response.length()));
		return nrr;
	}
		
	
	protected NetRequestResult getNetRequestResult(String id, Source source) throws Exception {
		String flags = "-x -r ";
		String query = flags + id;
		Request req = new Request(registry, flags, id, Request.NET_TYPE, query);
		LOG.fine("Requesting: " + query);
		NetRequestResult nrr = whoisMgr.getWhoisEngine().processRequest(req, source);
		LOG.fine("Received response. source=" + nrr.responseSource + (nrr.response == null ? ", response=null" : ", length=" + nrr.response.length()));
		return nrr;
	}
	


	protected void parseNets(Net parent, NetRequestResult nrr) throws IOException {
		
		if (nrr.response == null) {
			nrr.nets = null;
			return;
		}
		
		FastList<Net> nets = new FastList<Net>();
		
		String rangeStr = null;
		AddressRange range = null;
		String name = null;
		String org = null;
		Registry netReg = registry; // default
		
		BufferedReader reader = new BufferedReader(new StringReader(nrr.response));
		
		String line;
		boolean inNet = false;
		boolean inError = false;
		TextBuilder errorBuf = null;
		while ((line = reader.readLine()) != null) {
			if (!inError) {
				if (inError = checkForError(line)) {
					errorBuf = new TextBuilder();
					errorBuf.append("Error returned from registry for net request: " + nrr.request.getRequest());
				}
			}
			if (inError) {
				errorBuf.append("\n" + line);
			} else {
				if (inNet) {
					if (line.startsWith("netname:")) {
						name = line.substring(8).trim();
					} else if (line.startsWith("descr:")) {
						org = line.substring(6).trim();
						
						Net net = new Net(parent, range, name, netReg, rangeStr, org);
						nets.add(net);
						
						range = null;
						rangeStr = null;
						
						if (org.equals("Japan Network Information Center"))
							netReg = Registry.JPNIC;
						else
							netReg = registry;
						org = null;
						inNet = false;
					}
				} else if (line.indexOf("inetnum:") == 0) {
					// found a net
					// now expect the following fields in order:
					//      inetnum, netname, descr, country, admin-c, tech-c, status, mnt-by, source
					rangeStr = line.substring(8).trim();
					if (rangeStr.indexOf('-') < 0) { // check the next line for the rest of the address range
						reader.mark(1024);
						String nextLine = reader.readLine();
						if (nextLine == null)
							break;
						if (nextLine.indexOf('-') >= 0)
							rangeStr += nextLine;
						else
							reader.reset();
					}
					range = AddressRange.parseAddressRange(rangeStr);
					inNet = true;
				}
			}
		}
		reader.close();
		
		if (errorBuf != null) {
			String error = errorBuf.toString();
			LOG.warning(error);
			throw new IOException("Problem parsing nets: " + error);
		}
		
		nrr.nets = nets.toArray(new Net[nets.size()]);
	}
	
	protected String getNetInfo(Net net, NetRequestResult nrr) throws IOException {
		if (nrr.response == null)
			return null;
		
		TextBuilder text = new TextBuilder();
		
		BufferedReader reader = new BufferedReader(new StringReader(nrr.response));

		String line;
		boolean inNet = false;
		boolean done = false;
		boolean inError = false;
		TextBuilder errorBuf = null;
		while ((line = reader.readLine()) != null && !done) {
			if (!inError) {
				if (inError = checkForError(line)) {
					errorBuf = new TextBuilder();
					errorBuf.append("Error returned from registry for net info request: " + nrr.request.getRequest());
				}
				if (line.indexOf("Incorrect usage of flags") >= 0) {
					LOG.info("Incorrect usage of flags for request: " + nrr.request);
				}
			}
			if (inError) {
				errorBuf.append("\n" + line);
			} else {
				if (line.indexOf("inetnum:") == 0) {
					if (inNet) {
						inNet = false;
						done = true;
					} else {
						// found a net
						// check if it is the one we're looking for...
						String rangeStr = line.substring(line.indexOf(':') + 1).trim();
						AddressRange range = AddressRange.parseAddressRange(rangeStr);
						if (range.equals(net.getRange()))
							inNet = true;
					}
				}
				if (inNet) {
					text.append(line);
					text.append('\n');
				}
			}
		}
		reader.close();
		
		if (errorBuf != null) {
			String error = errorBuf.toString();
			LOG.warning(error);
			throw new IOException("Problem parsing nets: " + error);
		}
		
		return text.toString();
	}
	
	private boolean checkForError(String line) {
		boolean error = false;
		if (line.startsWith("%ERROR:")) {
			try {
				String numStr = line.split(":")[1];
				int num = Integer.parseInt(numStr);
				if (num > 101)
					error = true;
			} catch (Throwable t) {
				LOG.log(Level.INFO, "Problem parsing error message number from line: '" + line + "'", t);
				error = true;
			}
		}
		return error;
	}

}
