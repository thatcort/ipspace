/*
 * Created on 11-Jan-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.data;

public abstract class RegObj {

	private Registry registry;
	private String name;
	private String id;
	private Long dbid;
	
	public RegObj(Registry registry, String name, String id) {
		this.registry = registry;
		this.name = name;
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public synchronized String getName() {
		return name;
	}
	
	public synchronized void setName(String name) {
		this.name = name;
	}

	
	public Registry getRegistry() { return registry; }
	
	
	public void setDBID(Long dbid) {
		this.dbid = dbid;
	}
	public Long getDBID() {
		return dbid;
	}

}
