package com.liminal.ipspace.data;

import java.util.Comparator;

import com.liminal.ipspace.IPSpace;


/**
 * @author bcort
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Net extends RegObj implements Comparable<Net> {

	public static class NetRangeComparator implements Comparator<Net> {
		public int compare(Net o1, Net o2) {
			return o1.getRange().compareTo(o2.getRange());
		}
		
	}

	private Net parent;
	
	private String org;
	
	private AddressRange range;
	
	private int level;
	
//	private volatile String info; // the full whois text
	
	public Net(Net parent, AddressRange range, Registry registry, String id) {
		this(parent, range, null, registry, id, null);
	}
	
	public Net(Net parent, AddressRange range, String name, Registry registry, String id, String org) {
		super(registry, name, id);
		this.parent = parent;
		this.range = range;
		this.org = org;
		this.level = (parent == null ? 0 : parent.getLevel() + 1);
		if (range.min() < 0 || range.max() > 0xFFFFFFFFL)
			throw new IllegalArgumentException("address out of range. must be positive 32 bit #.");
	}
	
	public Net getParent() { return parent; }
	
	/** This method should ONLY be used by DataManager when absolutely necessary (and when it won't fuck everything up)! */
	protected void setParent(Net parent) {
		this.parent = parent;
		this.level = parent.getLevel()+1;
	}
	
	public AddressRange getRange() { return range; }
	
//	public boolean isHost() { return range.min() == range.max(); }
	
	public String getOrg() { return org; }
	
	public void setOrg(String org) {
		this.org = org;
	}
	
	public int getLevel() { return level; }
	
	/** Will return null if not known yet, but will then try to retrieve it. */
	public Net[] getSubnets() {
		return IPSpace.getInstance().getDataManager().getSubnets(this);
	}
	
	/** Will return null if not known yet, but will then try to retrieve it. */
	public String getWhoisInfo() {
		return IPSpace.getInstance().getDataManager().getNetInfo(this);
	}
	
	public int compareTo(Net n) {
		if (n == this)
			return 0;
		return range.compareTo(n.range);
	}
	
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Net))
			return false;
		return ((Net)o).range.equals(range);
	}
	
	public boolean intersects(Net n) {
		return range.intersects(n.range);
	}
	
	public String toString() {
		return "Net: " + getName() + ", " + getRegistry() + ", "  + getId() + ", " + org + ", " + range; 
	}
	
	public boolean isAllocated() { return true; }
	
	/** Returns whether DataManager has the nets stored */
	public boolean hasSubnetsReady() {
		return IPSpace.getInstance().getDataManager().hasStoredSubnets(this);
	}
	
//	// TODO: maybe add a separate NetInfo class to encapsulate more info
//	public String getInfo() { return info; }
//	public void setInfo(String info) {
//		this.info = info;
//	}
	
//	public synchronized NetInfo getNetInfo() { return info; }
//	
//	public synchronized void addNetInfo(NetInfo info) {
//	if (!info.getNet().equals(this))
//	throw new IllegalArgumentException("NetInfo not for this net!");
//	if (info == null)
//	this.info = info;
//	else
//	this.info.add(info);
//	}
}

