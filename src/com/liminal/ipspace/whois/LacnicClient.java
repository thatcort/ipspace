/*
 * Created on 28-Feb-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import static com.liminal.ipspace.data.Registry.LACNIC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import javolution.text.TextBuilder;

import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.whois.WhoisManager.Source;

public class LacnicClient extends InternationalClient {

	public LacnicClient(WhoisManager whoisMgr) {
		super(whoisMgr, LACNIC);
	}

	/** lacnic does not support subnet query requests */
	public NetRequestResult getSubNets(Net net, Source sources)
			throws Exception {
		Request request = new Request(LACNIC, "", net.getId(), Request.SUBNET_TYPE, net.getRange().toString());
		NetRequestResult result = new NetRequestResult(request, null, Source.LOCAL);
		result.nets = new Net[0];
		return result;
	}
	
	/** requires that the id be valid for lacnic */
	public NetRequestResult getNet(Net parent, String id, Source sources) throws Exception {
		NetRequestResult nrr = getNetRequestResult(id, sources);
		parseNets(parent, nrr);
		return nrr;
	}

	public NetInfoRequestResult getNetInfo(Net net, Source sources) throws Exception {
		// don't assume the net has
		String id = net.getRange().getCidrRanges()[0];
		NetRequestResult nrr = getNetRequestResult(id, sources);
		String info = getNetInfo(net, nrr);
		NetInfoRequestResult nirr = new NetInfoRequestResult(nrr.request, nrr.response, nrr.responseSource, info);
		return nirr;
	}

	protected NetRequestResult getNetRequestResult(String id, Source sources) throws Exception {
		Request req = new Request(LACNIC, "", id, Request.NET_TYPE, id);
		return whoisMgr.getWhoisEngine().processRequest(req, sources);
	}
	
//	/** lacnic does not support subnet query requests */
//	public NetRequestResult getSubNets(Net net, Set<Source> sources)
//			throws Exception {
//		Request request = new Request(getServer(), "", net.getId(), Request.SUBNET_TYPE, net.getRange().toString());
//		NetRequestResult result = new NetRequestResult(request, null, Source.LOCAL);
//		return result;
//	}
//	
//	/** requires that the id be valid for lacnic */
//	public NetRequestResult getNet(Net parent, String id, Set<Source> sources) throws Exception {
//		NetRequestResult nrr = getNetRequestResult(id, sources);
//		parseNets(parent, nrr);
//		return nrr;
//	}
//
//	public NetInfoRequestResult getNetInfo(Net net, Set<Source> sources) throws Exception {
//		// don't assume the net has
//		String id = net.getRange().getCidrRanges()[0];
//		NetRequestResult nrr = getNetRequestResult(id, sources);
//		String info = getNetInfo(net, nrr);
//		NetInfoRequestResult nirr = new NetInfoRequestResult(nrr.request, nrr.response, nrr.responseSource, info);
//		return nirr;
//	}
//
//	protected NetRequestResult getNetRequestResult(String id, Set<Source> sources) throws Exception {
//		Request req = new Request(server, "", id, Request.NET_TYPE, id);
//		return whoisMgr.getWhoisEngine().processRequest(req, sources);
//	}
	
	protected String getNetInfo(Net net, NetRequestResult nrr) throws IOException {
		TextBuilder text = new TextBuilder();
		
		if (nrr.response == null)
			return null;
		
		BufferedReader reader = new BufferedReader(new StringReader(nrr.response));
		
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.length() == 0) {
				text.append('\n');
			} else {
				char c0 = line.charAt(0);
				if (c0 != '%' && c0 != '#') {
					text.append(line);
					text.append('\n');
				}
			}
		}
		reader.close();
		
		return text.toString();
	}
}
