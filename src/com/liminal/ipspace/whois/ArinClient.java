/*
 * Created on 20-Dec-2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.logging.Logger;

import javolution.lang.TextBuilder;

import com.liminal.ipspace.data.Address;
import com.liminal.ipspace.data.AddressRange;
import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.data.Registry;

import static com.liminal.ipspace.data.Registry.*;

import com.liminal.ipspace.whois.Request.CallingMethod;
import com.liminal.ipspace.whois.WhoisManager.Source;

public class ArinClient implements WhoisClient {

	private static final Logger LOG = Logger.getLogger(ArinClient.class.getName());
	
	private WhoisManager whoisMgr;
	
	public ArinClient(WhoisManager whoisMgr) {
		this.whoisMgr = whoisMgr;
	}

	public String getServer() { return ARIN.getWhoisServer(); }

	
	public NetRequestResult getNet(Net parent, String id, Source source) throws Exception {
		NetRequestResult nrr = getSubnetResult(id, source, CallingMethod.NET_METHOD, parent);
		Net net = parseNet(parent, nrr.response);
		if (net == null)
			nrr.nets = null;
		else
			nrr.nets = new Net[] {net};
		return nrr;		
	}

	public NetInfoRequestResult getNetInfo(Net net, Source source) throws Exception {
		NetRequestResult nrr = getSubnetResult(net.getId(), source, CallingMethod.INFO_METHOD, net);
		String info = getNetInfo(nrr);
		NetInfoRequestResult nirr = new NetInfoRequestResult(nrr.request, nrr.response, nrr.responseSource, info);
		return nirr;
	}

	public NetRequestResult getSubNets(Net net, Source source) throws Exception {
		NetRequestResult nrr = getSubnetResult(net.getId(), source, CallingMethod.SUBNET_METHOD, net);
		Net[] nets = parseSubNets(net, nrr.response);
		nrr.nets = nets;
		return nrr;
	}
	
	private NetRequestResult getSubnetResult(String id, Source source, CallingMethod method, Net net) throws Exception {
		String flags = "> N ! ";
		String query = flags + id;
		Request req = new Request(ARIN, flags, id, Request.SUBNET_TYPE, query, method, net);
		LOG.fine("Requesting: " + query);
		NetRequestResult nrr = whoisMgr.getWhoisEngine().processRequest(req, source);
		LOG.fine("Received response. source=" + nrr.responseSource + ", length=" + (nrr.response == null ? "null" : nrr.response.length()));
		return nrr;
	}


	
	private Net parseNet(Net parent, String response) throws IOException {
		if (response == null)
			return null;
		
		AddressRange range = null;
		String name = null;
		String handle = null;
		String org = null;
		Registry registry = ARIN; // default
		
		BufferedReader reader = new BufferedReader(new StringReader(response));
		
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("#") || line.startsWith("Subdelegations")) { // we might have a subnet response or a net response.
				break;
			}
			int colInd = line.indexOf(':');
			if (line.length() == 0 || colInd < 0)
				continue;
			
			String ln = new String(line.substring(0, colInd)).trim();
			String lv = new String(line.substring(colInd + 1)).trim();
			
			if (ln.equals("NetRange")) {
				range = parseRange(lv);
			} else if (ln.startsWith("NetName")) {
				name = lv;
			} else if (ln.startsWith("NetHandle")) {
				handle = lv;
			} else if (ln.startsWith("OrgName")) {
				org = lv;
				Registry orgReg = getOrgNameRegistry(org);
				if (orgReg != null)
					registry = orgReg;
			} else if (ln.startsWith("ReferralServer")) {
				lv = lv.toLowerCase();
				if (lv.indexOf("apnic") >= 0) {
					registry = APNIC;
				} else if (lv.indexOf("ripe") >= 0) {
					registry = RIPE;
				} else if (lv.indexOf("lacnic") >= 0) {
					registry = LACNIC;
				} else if (lv.indexOf("afrinic") >= 0) {
					registry = AFRINIC;
				} else if (lv.indexOf("jnic") >= 0) {
					registry = JPNIC;
				}
			}
		}
		reader.close();
		
//		// override the handle (id) for non-arin nets:
//		if (!registry.equals(WhoisManager.ARIN)) {
//			if (registry.equals(WhoisManager.LACNIC)) {
//				handle = range.getCidrRanges()[0];
//			} else {
//				handle = range.getMinAddress().toString() + " - " + range.getMaxAddress().toString();
//			}
//		}
		
		Net net = new Net(parent, range, name, registry, handle, org);
		
		return net;
	}

	
	// only expecting
	private Net[] parseSubNets(Net parent, String response) throws IOException {
		if (response == null) {
			return null;
		}
		
		ArrayList<Net> subList = new ArrayList<Net>();

		BufferedReader reader = new BufferedReader(new StringReader(response));

		String line;
		String prevLine = null; // sometimes need to prepend the previous onto the next to get the right result
		boolean parseLines = false;
		while ((line = reader.readLine()) != null) {
			if (parseLines) {
				if (line.startsWith("#")) {
					parseLines = false;
					break;
				}
				
				if (prevLine != null) {
					line = prevLine + " " + line;
					prevLine = null;
				}
				
				// each line is of the form: orgName netName (netHandle) rangestart - rangeend
				// orgName may contain spaces, the others may not
				line = line.trim();
				if (line.length() == 0)
					continue;
					
				// check for the info breaking across lines:
				boolean combineLine = false;
				int nameEnd = -1;
				int handleStart = -1;
				int handleEnd = -1;
				nameEnd = line.lastIndexOf(" (NET");
				if (nameEnd < 0) {
					combineLine = true;
				} else { 
					handleStart = nameEnd + 2;
					handleEnd = line.indexOf(')', handleStart);
					if (handleEnd < 0) {
						combineLine = true;
					} else {
						int dashInd = line.indexOf('-', handleEnd);
						if (dashInd < 0) {
							combineLine = true;
						} else {
							int dotCount = 0;
							for (int i=dashInd+1; i < line.length(); i++) {
								if (line.charAt(i) == '.')
									dotCount++;
							}
							if (dotCount < 3)
								combineLine = true;
						}
					}
				}

				if (combineLine) {
					prevLine = line;
					continue;
				}
				
				int nameStart = line.lastIndexOf(' ', nameEnd-1) + 1;
				String name = new String(line.substring(nameStart, nameEnd));
				String org = new String(line.substring(0, nameStart-1));
				String handle = new String(line.substring(handleStart, handleEnd));
				AddressRange range = AddressRange.parseAddressRange(line.substring(handleEnd + 1));
//System.out.println(org + "; " + name + "; " + handle + "; " + range);
				Registry registry = getOrgNameRegistry(org);
				if (registry == null) {
					registry = ARIN;
				}
				Net net = new Net(parent, range, name, registry, handle, org);
				subList.add(net);
			} else if (line.startsWith("Subdelegations")) {
				parseLines = true;
			}
		}
		reader.close();
		
		return subList.toArray(new Net[subList.size()]);
	}
	
	private AddressRange parseRange(String str) {
		int hypInd = str.indexOf('-');
		String s0 = new String(str.substring(0, hypInd)).trim();
		String s1 = new String(str.substring(hypInd+1)).trim();
		Address add0 = new Address(s0);
		Address add1 = new Address(s1);
		return new AddressRange(add0, add1);
	}
	
	/** returns null if unable to determine */
	private Registry getOrgNameRegistry(String org) {
		if (org.indexOf("African Network Information Center") >= 0)
			return AFRINIC;
		if (org.indexOf("Asia Pacific Network Information Centre") >= 0)
			return APNIC;
		if (org.indexOf("Internet Assigned Numbers Authority") >= 0)
			return IANA;
		if (org.indexOf("RIPE Network Coordination Centre") >= 0)
			return RIPE;
		if (org.indexOf("Latin American and Caribbean IP address Regional Registry") >= 0)
			return LACNIC;
		if (org.indexOf("Japan Network Information Center") >= 0) {
			return JPNIC;
		}
		return null;
	}
	
	private String getNetInfo(NetRequestResult nrr) throws IOException {
		if (nrr.response == null)
			return null;
		
		TextBuilder text = new TextBuilder();
		
		BufferedReader reader = new BufferedReader(new StringReader(nrr.response));
		
		String line;
		boolean done = false;
		while ((line = reader.readLine()) != null && !done) {
			if (line.indexOf("Incorrect usage of flags") >= 0) {
				System.out.println("aaaa");
//				LOG.info("Incorrect usage of flags for request: " + nrr.request);
			}
			if (line.startsWith("Subdelegations")) {
				done = true;
			} else {
				text.append(line);
				text.append('\n');
			}
		}
		reader.close();
		
		return text.toString();
	}



	
}
