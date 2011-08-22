/*
 * Created on 7-Jan-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.data.Registry;

public class Request {

	public static final int SUBNET_TYPE = 0;
	public static final int NET_TYPE = 1;
	
	public static enum CallingMethod {
		NET_METHOD,
		SUBNET_METHOD,
		INFO_METHOD
	}
	
	private Registry registry;
	private String flags;
	private String objid;
	private int type; // type made to server: net or subnet
	private String request;
	private CallingMethod method;
	private Net net;
	
	public Request(Registry registry, String flags, String objid, int type, String request, CallingMethod method, Net net) {
		this.registry = registry;
		this.flags = flags;
		this.objid = objid;
		this.type = type;
		this.request = request;
		this.method = method;
		this.net = net;
	}

	public String getFlags() {
		return flags;
	}

	public String getObjid() {
		return objid;
	}
	
	public int getType() {
		return type;
	}

	public String getRequest() {
		return request;
	}

	public Registry getRegistry() {
		return registry;
	}
	
	public String toString() {
		return "Request: registry=" + registry + ", request=" + request;
	}
	
	public CallingMethod getCallingMethod() {
		return method;
	}
	
	public Net getNet() { return net; }
//
//	
//	public static class NetRequest extends Request {
//		public NetRequest(Registry registry, String flags, String objid, int type, String request) {
//			super(registry, flags, objid, type, request);
//		}
//		public Net getParent() { return net; }
//	}
//	public static class SubnetRequest extends Request {
//		public SubnetRequest(Registry registry, String flags, String objid, int type, String request) {
//			super(registry, flags, objid, type, request, RequestType.SUBNET_REQUEST);
//		}
//		public 
//	}
//	public static class InfoRequest extends Request {
//		public InfoRequest(Registry registry, String flags, String objid, int type, String request) {
//			super(registry, flags, objid, type, request, RequestType.INFO_REQUEST);
//		}
//	}
}
