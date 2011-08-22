/*
 * Created on 12-Jan-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.whois.WhoisManager.Source;

public class GetSubnetResult extends GetNetResult {

	private Net[] subnets;
	private String[] subnetInfos;
	
	public GetSubnetResult(Net parent, Net[] subnets, Source responseSource) {
		this(parent, null, subnets, null, responseSource);
	}
	public GetSubnetResult(Net parent, Net[] subnets, String[] subnetInfos, Source responseSource) {
		this(parent, null, subnets, subnetInfos, responseSource);
	}
	public GetSubnetResult(Net parent, String parentInfo, Net[] subnets, Source responseSource) {
		this(parent, parentInfo, subnets, null, responseSource);
	}
	
	public GetSubnetResult(Net parent, String parentInfo, Net[] subnets, String[] subnetInfos, Source responseSource) {
		super(parent, parentInfo, responseSource);
		this.subnets = subnets;
		this.subnetInfos = subnetInfos;
	}

	public Net[] getSubnets() {
		return subnets;
	}
	
	public String[] getSubnetInfos() {
		return subnetInfos;
	}

}
