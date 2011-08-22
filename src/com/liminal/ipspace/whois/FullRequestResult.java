/*
 * Created on 11-Apr-07
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import java.util.List;

import javolution.util.FastTable;

import com.liminal.ipspace.data.NetTree;

public class FullRequestResult {

	private Request request;
	
	private NetTree netTree;
	
	private List<NetInfo> infos;
	
	public FullRequestResult(Request request) {
		this.request = request;
		netTree = new NetTree(request.getNet());
		netTree.setFillGaps(false);
		infos = new FastTable<NetInfo>(3);
	}
	
	public Request getRequest() {
		return request;
	}
	
	public NetTree getNetTree() {
		return netTree;
	}
	
	public NetInfo[] getNetInfos() {
		return infos.toArray(new NetInfo[infos.size()]);
	}
	
	public void addNetInfos(NetInfo info) {
		infos.add(info);
	}
	
	
}
