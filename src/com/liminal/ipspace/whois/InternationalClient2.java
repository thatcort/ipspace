/*
 * Created on 7-Apr-07
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.logging.Level;

import javolution.text.TextBuilder;
import javolution.util.FastTable;

import com.liminal.ipspace.data.AddressRange;
import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.data.Registry;
import com.liminal.ipspace.whois.Request.CallingMethod;

public class InternationalClient2 extends WhoisClient2 {
	
	protected boolean canQuerySubnets = true;

	public InternationalClient2(Registry registry, WhoisDB2 db) {
		super(registry, db);
	}
	
	
	protected Request getNetRequest(Net parent, String id) {
		return getRequest(parent, id, Request.NET_TYPE, CallingMethod.NET_METHOD);
	}
	
	protected Request getSubnetsRequest(Net parent) {
		return getRequest(parent, parent.getId(), Request.SUBNET_TYPE, CallingMethod.SUBNET_METHOD);
	}

	protected Request getNetInfoRequest(Net net) {
		return getRequest(net, net.getId(), Request.NET_TYPE, CallingMethod.INFO_METHOD);
	}

	private Request getRequest(Net net, String id, int type, CallingMethod method) {
		String flags;
		if (type == Request.SUBNET_TYPE)
			flags = "-m -r ";
		else
			flags = "-x -r ";
		String query = flags + id;
		return new Request(registry, flags, id, type, query, method, net);
	}
	
	@Override
	public GetNetInfoResult getNetInfoRemotely(Net net) throws Exception {
		// a bit complicated...
		// if the net's parent has the same registry as the net in question,
		// then first check a subnet query on the parent (we are more likely to have this result already)
		// if that doesn't work (possibly because the sources don't allow a net request)
		// then try a getNet query on the net itself

		// I changed this slightly from the original behaviour: before it would try one, then the other.
		// Now it uses the flag 'canQuerySubnets' to decide if it should try
		// LACNIC does not allow subnet queries
		if (canQuerySubnets && net.getParent().getRegistry() == net.getRegistry()) {
			getSubnetsRemotely(net.getParent());
			return getNetInfoLocally(net);
		} else {
			return super.getNetInfoRemotely(net);
		}
	}
	
	
	public FullRequestResult parseResponse(Request request, BufferedReader response) throws IOException {
		FullRequestResult result = new FullRequestResult(request);
		
		List<String> infos = getNetInfos(request, response);
		for (String info : infos) {
			Net net = parseNet(request, request.getNet(), info);
			if (net == null) {
				LOG.warning("Unable to read network: " + info);
				continue;
			}
			boolean added = result.getNetTree().addNet(net, false);
			if (added)
				result.addNetInfos(new NetInfo(net, info));
			else
				LOG.info("Ignoring duplicate network definition: " + info);
		}
		return result;
	}

	protected List<String> getNetInfos(Request request, BufferedReader reader) throws IOException {
		if (reader == null)
			return null;
		
		TextBuilder text = new TextBuilder();
		
		List<String> infos = new FastTable<String>();
		
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
				if (line.indexOf("inetnum:") == 0) {
					if (inNet) {
						inNet = false;
						infos.add(text.toString());
						text.clear();
					}
					
					inNet = true;
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
	

	protected Net parseNet(Request request, Net parent, String info) throws IOException {
		
		if (info == null) {
			return null;
		}
		
		
		String rangeStr = null;
		AddressRange range = null;
		String name = null;
		String org = null;
		Registry netReg = registry; // default
		
		BufferedReader reader = new BufferedReader(new StringReader(info));
		
		String line;
		boolean inError = false;
		TextBuilder errorBuf = null;
		while ((line = reader.readLine()) != null) {
			if (!inError) {
				if (inError = checkForError(line)) {
					errorBuf = new TextBuilder();
					errorBuf.append("Error returned from registry for net request: " + request);
				}
			}
			if (inError) {
				errorBuf.append("\n" + line);
			} else {
				if (line.indexOf("inetnum:") == 0) {
					// found a net
					// now expect the following fields in order:
					//      inetnum, netname, descr, country, admin-c, tech-c, status, mnt-by, source
					rangeStr = line.substring(8).trim();
					boolean isCidr = rangeStr.indexOf('/') >= 0;
					if (!isCidr && rangeStr.indexOf('-') < 0) { // check the next line for the rest of the address range
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
				} else 	if (line.startsWith("netname:")) {
					name = line.substring(8).trim();
				} else if (line.startsWith("descr:")) {
					if (name == null)
						return null;
					
					org = line.substring(6).trim();
					
					if (org.equals("Japan Network Information Center"))
						netReg = Registry.JPNIC;
					else if (name.equals("IANA-BLK"))
						netReg = Registry.IANA;
					else
						netReg = registry;


					
				}
			}
		}
		reader.close();
		
		if (errorBuf != null) {
			String error = errorBuf.toString();
			LOG.warning(error);
			throw new IOException("Problem parsing nets: " + error);
		}
		
		Net net = new Net(parent, range, name, netReg, rangeStr, org);
		return net;
	}
	
	private boolean checkForError(String line) {
		boolean error = false;
		error = checkLineForTimeout(line);
		if (!error && line.startsWith("%ERROR:")) {
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
