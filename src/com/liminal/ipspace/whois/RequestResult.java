/*
 * Created on 31-Mar-07
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

public class RequestResult<T extends Result> {

	private Request request;
	private T result;
	
	public RequestResult(Request request, T result) {
		this.request = request;
		this.result = result;
	}
	
	public Request getRequest() {
		return request;
	}

	public T getResult() {
		return result;
	}
}
