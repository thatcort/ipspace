package com.liminal.ipspace.whois;

import static com.liminal.ipspace.data.Registry.JPNIC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

import javolution.text.TextBuilder;
import javolution.util.FastList;

import com.liminal.ipspace.data.Address;
import com.liminal.ipspace.data.AddressRange;
import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.data.Registry;
import com.liminal.ipspace.whois.WhoisManager.Source;

public class JpnicClient implements WhoisClient {

	protected final Logger LOG = Logger.getLogger(JpnicClient.class.getName());;
	
	private WhoisManager whoisMgr;
	
	public JpnicClient(WhoisManager whoisMgr) {
		this.whoisMgr = whoisMgr;
	}
	
	public String getServer() {
		return JPNIC.getWhoisServer();
	}

	public NetRequestResult getNet(Net parent, String id, Source source) throws UnknownHostException, SQLException, IOException {
		String query = "NET " + id + "/e";
		Request req = new Request(JPNIC, "NET", id, Request.NET_TYPE, query);
		LOG.fine("Requesting: " + query);
		NetRequestResult nrr = whoisMgr.getWhoisEngine().processRequest(req, source);
		LOG.fine("Received response. source=" + nrr.responseSource + (nrr.response == null ? ", response=null" : ", length=" + nrr.response.length()));
		parseNets(parent, nrr);
		return nrr;
	}
	
	public NetRequestResult getSubNets(Net net, Source source) throws Exception {
		// assume that if the net's registry is jpnic, then we would have gotten its children
		// in the 'more specific info' section of the query for the parent
		// So, if we're here, then that means there are no subnets.
		if (net.getParent().getRegistry() == JPNIC) {
			NetRequestResult nrr = new NetRequestResult(null, "", Source.LOCAL);
			nrr.nets = new Net[0];
			return nrr;
		}
		ArrayList<Net> subnets = new ArrayList<Net>();
		TextBuilder text = new TextBuilder();
		AddressRange range = net.getRange();
		String[] cidrs = AddressRange.getCidrRanges(range.min(), range.max());
		for (int i=0; i<cidrs.length; i++) {
			String cidr = cidrs[i];
			NetRequestResult snrr = getSubNets(net, cidr, source);
			if (snrr.response != null) {
				if (snrr.nets != null) {
					for (Net n : snrr.nets)
						subnets.add(n);
				}
				text.append(snrr.response);
				text.append("\n\n");
			} else {
				text = null;
				break;
			}
		}
		Request req = new Request(JPNIC, "NET", net.getId(), Request.SUBNET_TYPE, "Multiple subnet queries");
		String response = (text != null ? text.toString() : null);
		NetRequestResult nrr = new NetRequestResult(req, response, source);
		if (text != null)
			nrr.nets = subnets.toArray(new Net[subnets.size()]);
		return nrr;
	}
	
	/** cidr is a portion of the range of the parent */
	private NetRequestResult getSubNets(Net parent, String cidr, Source source) throws Exception {
		int mask = 32;
		int slashInd = cidr.indexOf('/');
		if (slashInd > 0) {
			mask = Integer.parseInt(cidr.substring(slashInd + 1));
		}
		int subMask = ((mask + 8) / 8) * 8; // increase resolution by one octet and then make 256 calls!
		
		AddressRange cidrRange = AddressRange.parseAddressRange(cidr);
		int subRange = 1 << (32 - subMask);
		
		if (subMask >= 32)
			subMask = mask + 1;
		
		
		ArrayList<Net> subnets = new ArrayList<Net>();
		TextBuilder text = new TextBuilder();
		
		for (int i=0; i < 256; i++) {
			long subStart = cidrRange.min() + (i * subRange);
			Address subAddress = new Address(subStart);
			String subCidr = subAddress.toString() + "/" + subMask;
			NetRequestResult nrr = null;
			nrr = getNet(parent, subCidr, Source.LOCAL);
			if (nrr.response == null) {
				if (source == Source.REMOTE)
					nrr = getNet(parent, subCidr, Source.REMOTE);
			}
			if (nrr.nets == null) {
				Request req = new Request(JPNIC, "NET", cidr, Request.SUBNET_TYPE, "Multiple requests");
				NetRequestResult nullResult = new NetRequestResult(req, null, source);
				return nullResult;
			}
			for (Net n : nrr.nets)
				subnets.add(n);
			if (nrr.response != null) {
				text.append(nrr.response);
				text.append("\n\n");
			}
		}
		Request req = new Request(JPNIC, "NET", cidr, Request.SUBNET_TYPE, "Multiple requests");
		NetRequestResult nrr = new NetRequestResult(req, text.toString(), source);
		nrr.nets = subnets.toArray(new Net[subnets.size()]);
		return nrr;
	}
	
	
	public NetInfoRequestResult getNetInfo(Net net, Source source) throws Exception {
		String query = "NET " + net.getId();
		Request req = new Request(JPNIC, "NET", net.getId(), Request.NET_TYPE, query);
		LOG.fine("Requesting: " + query);
		NetRequestResult nrr = whoisMgr.getWhoisEngine().processRequest(req, source);
		LOG.fine("Received response. source=" + nrr.responseSource + (nrr.response == null ? ", response=null" : ", length=" + nrr.response.length()));
		String info = parseNetInfo(nrr);
		NetInfoRequestResult nirr = new NetInfoRequestResult(nrr.request, nrr.response, nrr.responseSource, info);
		return nirr;
	}
	
	private void parseNets(Net parent, NetRequestResult nrr) throws IOException {
		if (nrr.response == null) {
			nrr.nets = null;
			return;
		}
		
		FastList<Net> nets = new FastList<Net>();
		
		Net firstNet = null; // first net parsed -- if there is more specific net info given, then this net is the parent for those
		
		String rangeStr = null;
		AddressRange range = null;
		String name = null;
		String org = null;
		Registry registry = JPNIC; // default
		
		BufferedReader reader = new BufferedReader(new StringReader(nrr.response));

		String line;
		boolean inNet = false;
		
		boolean inMoreSpecific = false;
		boolean inLessSpecific = false;
		
		while ((line = reader.readLine()) != null) {
			
			if (checkForError(line)) {
				LOG.warning("Unable to parse response for net request: " + nrr.request.getRequest());
				throw new IOException("Problem parsing nets for request: " + nrr.request.getRequest());
			}
			
			if (line.indexOf("Less Specific Info.") >= 0) {
				inLessSpecific = true;
				inMoreSpecific = false;
			} else if (line.indexOf("More Specific Info.") >= 0) {
				inMoreSpecific = true;
				inLessSpecific = false;
			}
			
			if (inLessSpecific)
				continue;
			
			line = line.trim();
			if (line.length() == 0)
				continue;
			
			if (inNet) {
				int startVal = line.indexOf(']') + 1;
				if (line.indexOf("Network Name") >= 0) {
					name = line.substring(startVal).trim();
				} else if (line.indexOf("Organization") >= 0) {
					org = line.substring(startVal).trim();
					
					Net netParent = (inMoreSpecific ? firstNet : parent);
					
					Net net = new Net(netParent, range, name, registry, rangeStr, org);
					nets.add(net);
					
					range = null;
					rangeStr = null;
					registry = JPNIC;
					org = null;
					inNet = false;
				}
			} else if (line.indexOf("[Network Number]") >= 0) {
				// found a net
				rangeStr = line.substring(line.indexOf(']') + 1).trim();
				if (rangeStr.indexOf('/') < 0 && rangeStr.indexOf('-') < 0) { // check the next line for the rest of the address range (useful for internationalClient, so keeping this check for jpnic
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

		nrr.nets = nets.toArray(new Net[nets.size()]);
		
	}
	
	private String parseNetInfo(NetRequestResult nrr) throws Exception {
		if (nrr.response == null) {
			return null;
		}
		
		BufferedReader reader = new BufferedReader(new StringReader(nrr.response));
		
		TextBuilder text = new TextBuilder();

		String line;
		boolean inNet = false;
		boolean done = false;
		
		while ((line = reader.readLine()) != null && !done) {
			
			if (checkForError(line)) {
				LOG.warning("Unable to parse response for net request: " + nrr.request.getRequest());
				throw new IOException("Problem parsing nets for request: " + nrr.request.getRequest());
			}
			
			if (line.indexOf("Less Specific Info.") >= 0 || line.indexOf("More Specific Info.") >= 0) {
				done = true;
				continue;
			}
			
			line = line.trim();
			if (line.length() == 0)
				continue;
			
			if (inNet) {
				text.append(line);
				text.append('\n');
			} 

			if (line.indexOf("Network Information:") >= 0) {
				inNet = true;
			}
			
		}
		reader.close();
		
		if (text.length() == 0) {
			LOG.warning("No net info found in request: " + nrr.request.getRequest());
			return null;
		}
		
		return text.toString();
	}

	private boolean checkForError(String line) {
		return (line.indexOf("Too broad. Narrow the address range, please.") >= 0 || 
				line.indexOf("<JPNIC WHOIS HELP>") >= 0);
	}

	
	
}
