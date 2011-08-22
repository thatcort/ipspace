/*
 * Created on 9-May-07
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import static com.liminal.ipspace.data.Registry.JPNIC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;

import javolution.text.TextBuilder;
import javolution.util.FastTable;

import com.liminal.ipspace.data.Address;
import com.liminal.ipspace.data.AddressRange;
import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.whois.WhoisManager.Source;

public class JpnicClient2 extends WhoisClient2 {

	
	public JpnicClient2(WhoisDB2 db) {
		super(JPNIC, db);
	}
	
	@Override
	protected Request getNetRequest(Net parent, String id) {
		String query = "NET " + id + "/e";
		return new Request(JPNIC, "NET", id, Request.NET_TYPE, query, Request.CallingMethod.NET_METHOD, parent);
	}
	
	@Override
	protected Request getSubnetsRequest(Net parent) {
		// NOT USED
		return null;
	}
	
	
	@Override
	protected Request getNetInfoRequest(Net net) {
		String query = "NET " + net.getId() + "/e";
		return new Request(JPNIC, "NET", net.getId(), Request.NET_TYPE, query, Request.CallingMethod.INFO_METHOD, net);
	}

	
	@Override
	public GetSubnetResult getSubnetsRemotely(Net parent) throws Exception {
		GetSubnetResult result = new GetSubnetResult(parent, new Net[0], Source.LOCAL);
		return result;
//		// assume that if the net's registry is jpnic, then we would have gotten its children
//		// in the 'more specific info' section of the query for the parent
//		// So, if we're here, then that means there are no subnets.
//		if (parent.getParent().getRegistry() == JPNIC) {
//			GetSubnetResult result = new GetSubnetResult(parent, new Net[0], Source.LOCAL);
//			return result;
//		}
//
//		Request request = new Request(JPNIC, "NET", parent.getId(), Request.SUBNET_TYPE, "Multiple subnet queries", Request.CallingMethod.SUBNET_METHOD, parent);
//		FullRequestResult fullResult = new FullRequestResult(request);
//		AddressRange range = parent.getRange();
//		String[] cidrs = AddressRange.getCidrRanges(range.min(), range.max());
//if (cidrs.length <= 8) // getting connection reset after too many requests -- and no data back anyway :/
//		for (int i=0; i<cidrs.length; i++) {
//			String cidr = cidrs[i];
//			getSubnetRange(parent, cidr, fullResult);
//		}
//		
//		// order the netinfo strings into an array matching the order of the subnets
//		Net[] subnets = fullResult.getNetTree().getSubnets(parent);
//		NetInfo[] infos = fullResult.getNetInfos();
//		String parentInfo = null;
//		String[] infoStrs = null;
//		if (infos != null) {
//			for (NetInfo info : infos) {
//				Net n = info.getNet();
//				if (n == parent) {
//					parentInfo = info.getInfo();
//				} else {
//					for (int i = 0; i < subnets.length; i++) {
//						if (subnets[i] == n) {
//							if (infoStrs == null)
//								infoStrs = new String[subnets.length];
//							infoStrs[i] = info.getInfo();
//						}
//					}
//				}
//			}
//		}
//		
//		GetSubnetResult gsr = new GetSubnetResult(parent, parentInfo, subnets, infoStrs, Source.REMOTE); 
//		return gsr;
	}

	
	/** cidr is a portion of the range of the parent */
	private void getSubnetRange(Net parent, String cidr, FullRequestResult fullResult) throws Exception {
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
		
		BufferedReader reader = null;
		try {
			for (int i=0; i < 256; i++) {
				long subStart = cidrRange.min() + (i * subRange);
				Address subAddress = new Address(subStart);
				String subCidr = subAddress.toString() + "/" + subMask;
				
				Request request = getNetRequest(parent, subCidr);
				reader = getRemoteResponse(request);
				FullRequestResult subResult = parseResponse(request, reader);
				reader.close();
				reader = null;
				fullResult.getNetTree().addNets(subResult.getNetTree().getNets());
				for (NetInfo info : subResult.getNetInfos()) {
					fullResult.addNetInfos(info);
				}
			}
		} finally {
			if (reader != null)
				reader.close();
		}
	}

	
	@Override
	protected FullRequestResult parseResponse(Request request, BufferedReader response) throws IOException {
		if (response == null) {
			return null;
		}
		
		FullRequestResult result = new FullRequestResult(request);
		
		List<String> infos = getNetInfos(request, response);
		Net firstNet = null;
		if (!infos.isEmpty()) {
			firstNet = parseNet(request, request.getNet(), infos.get(0));
		} else
			return result;
		// the remaining nets all use have the first net as their parent (we don't parse the 'less specific' nets
		Iterator<String> iter = infos.iterator();
		iter.next();
		while (iter.hasNext()) {
			String info = iter.next();
			Net net = parseNet(request, firstNet, info);
			if (net != null) {
				result.getNetTree().addNet(net);
				result.addNetInfos(new NetInfo(net, info));
			} else {
				LOG.warning("Unable to parse net from JPNIC request " + request.getRequest() + "; portion: " + info);
			}
		}
		return result;
	}
	
	protected List<String> getNetInfos(Request request, BufferedReader reader) throws IOException {
		if (reader == null)
			return null;
		
		TextBuilder text = new TextBuilder();
		
		List<String> infos = new FastTable<String>();
		
		boolean inLessSpecific = false;
		
		String line;
		boolean inNet = false;
		boolean inError = false;
		TextBuilder errorBuf = null;
		while ((line = reader.readLine()) != null) {
			if (!inError) {
				if (inError = checkForError(line)) {
					errorBuf = new TextBuilder();
					errorBuf.append("Error returned from registry for net info request: " + request.getRequest());
				}
				if (line.indexOf("Incorrect usage of flags") >= 0) {
					LOG.info("Incorrect usage of flags for request: " + request);
				}
			}
			if (inError) {
				errorBuf.append("\n" + line);
			} else {
				if (line.indexOf("Network Name") >= 0 && !inLessSpecific) {
					if (inNet) {
						inNet = false;
						infos.add(text.toString());
						text.clear();
					}
					
					inNet = true;
				} else if (line.indexOf("Less Specific Info.") >= 0) {
					inLessSpecific = true;
					if (inNet)
						infos.add(text.toString());
					inNet = false;
					text.clear();
				} else if (line.indexOf("More Specific Info.") >= 0) {
					inLessSpecific = false;
					if (inNet)
						infos.add(text.toString());
					inNet = false;
					text.clear();
				}
				if (inNet) {
					text.append(line);
					text.append('\n');
				}
			}
		}
//		reader.close();
		
		if (text.length() > 0)
			infos.add(text.toString());
		text.clear();
		
		if (errorBuf != null) {
			String error = errorBuf.toString();
			LOG.warning(error);
			throw new IOException("Problem parsing nets: " + error);
		}
		
		return infos;
	}

	
	private Net parseNet(Request request, Net parent, String info) throws IOException {
		
		if (info == null)
			return null;
		

		String rangeStr = null;
		AddressRange range = null;
		String name = null;
		String org = null;

		String line;
		boolean inNet = false;
		
		BufferedReader reader = new BufferedReader(new StringReader(info));
		
		while ((line = reader.readLine()) != null) {
			
			if (checkForError(line)) {
				LOG.warning("Unable to parse response for net request: " + request.getRequest());
				throw new IOException("Problem parsing nets for request: " + request.getRequest());
			}
			
			line = line.trim();
			if (line.length() == 0)
				continue;
			
			if (inNet) {
				int startVal = line.indexOf(']') + 1;
				if (line.indexOf("Network Name") >= 0) {
					name = line.substring(startVal).trim();
				} else if (line.indexOf("Organization") >= 0) {
					org = line.substring(startVal).trim();
					
					return new Net(parent, range, name, registry, rangeStr, org);
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

		return null;
	}
	
	
	private boolean checkForError(String line) {
		return (line.indexOf("Too broad. Narrow the address range, please.") >= 0 || 
				line.indexOf("<JPNIC WHOIS HELP>") >= 0);
	}

	
}

