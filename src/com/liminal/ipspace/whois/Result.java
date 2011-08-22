/*
 * Created on 6-Apr-07
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import com.liminal.ipspace.whois.WhoisManager.Source;

public abstract class Result {
	
	private Source source;
	
	public Result(Source source) {
		this.source = source;
	}
	
	public Source getResponseSource() {
		return source;
	}
}
