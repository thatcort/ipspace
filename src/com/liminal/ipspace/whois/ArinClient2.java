/*
 * Created on 6-Apr-07
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import static com.liminal.ipspace.data.Registry.AFRINIC;
import static com.liminal.ipspace.data.Registry.APNIC;
import static com.liminal.ipspace.data.Registry.ARIN;
import static com.liminal.ipspace.data.Registry.IANA;
import static com.liminal.ipspace.data.Registry.JPNIC;
import static com.liminal.ipspace.data.Registry.LACNIC;
import static com.liminal.ipspace.data.Registry.RIPE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import javolution.text.TextBuilder;
import javolution.util.FastTable;

import com.liminal.ipspace.data.Address;
import com.liminal.ipspace.data.AddressRange;
import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.data.Registry;
import com.liminal.ipspace.whois.Request.CallingMethod;

public class ArinClient2 extends WhoisClient2 {

	
	public ArinClient2(WhoisDB2 db) {
		super(ARIN, db);
	}

	public Request getNetRequest(Net parent, String id) {
		return getRequest(parent, id, CallingMethod.NET_METHOD);
	}
	
	public Request getSubnetsRequest(Net parent) {
		return getRequest(parent, parent.getId(), CallingMethod.SUBNET_METHOD);
	}

	public Request getNetInfoRequest(Net net) {
		return getRequest(net, net.getId(), CallingMethod.INFO_METHOD);
	}
	
	private Request getRequest(Net net, String id, CallingMethod method) {
		String flags = "n ! > ";
		String query = flags + id;
		return new Request(ARIN, flags, id, Request.SUBNET_TYPE, query, method, net);		
	}

	
	

	@Override
	protected FullRequestResult parseResponse(Request request, BufferedReader reader) throws IOException {
		if (reader == null) {
			return null;
		}
		
		try {
			String info;
			Net infoNet; // net that we have detailed info on
			
			info = getNetInfo(reader);
			if (request.getCallingMethod() != CallingMethod.INFO_METHOD) {
				Net parent = request.getNet();
				infoNet = parseNet(parent, info);
			} else {
				infoNet = request.getNet();
			}
			NetInfo netInfo = new NetInfo(infoNet, info);
			
			Net[] subnets = parseSubnets(infoNet, reader, true);

			FullRequestResult frr = new FullRequestResult(request);
			if (netInfo != null)
				frr.addNetInfos(netInfo);
			if (infoNet != null)
				frr.getNetTree().addNet(infoNet);
			if (subnets != null)
				frr.getNetTree().addNets(subnets);
			
			return frr;
		} finally {
			reader.close();
		}
	}

	// reads until the end of the net details and then stops
	private Net parseNet(Net parent, String string) throws IOException {
		if (string == null)
			return null;
		
		AddressRange range = null;
		String name = null;
		String handle = null;
		String org = null;
		Registry registry = ARIN; // default
		
		BufferedReader reader = new BufferedReader(new StringReader(string));
		
		boolean pastHeader = false;
		
		String line;
		while ((line = reader.readLine()) != null) {
			if (checkLineForTimeout(line))
				return null;
			
			if (!pastHeader && line.startsWith("#"))
				continue;
			pastHeader = true;
			
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
	
	// startImmediately will start parsing the from the first line without trying to find the 'subdelegations' marker line
	private Net[] parseSubnets(Net parent, BufferedReader reader, boolean startImmediately) throws IOException {
		if (reader == null) {
			return null;
		}
		
		List<Net> subList = new FastTable<Net>();

		String line;
		String prevLine = null; // sometimes need to prepend the previous onto the next to get the right result
		boolean parseLines = startImmediately;
		while ((line = reader.readLine()) != null) {
			if (checkLineForTimeout(line))
				return null;
			
			if (parseLines) {
				if (line.startsWith("#")) {
LOG.info("Message in response" + line);
continue;
//					parseLines = false;
//					break;
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
		
		return subList.toArray(new Net[subList.size()]);
	}
	
	/** reads until the end of the net info */
	private String getNetInfo(BufferedReader reader) throws IOException {
		if (reader == null)
			return null;
		
		TextBuilder text = new TextBuilder();
		
		String line;
		boolean done = false;
		while (!done && (line = reader.readLine()) != null) {
			if (checkLineForTimeout(line))
				return null;
			
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
		
		return text.toString();
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

	
}
