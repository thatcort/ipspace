/*
 * Created on 14-Mar-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastMap;
import javolution.util.FastSet;
import javolution.util.FastTable;

public class NetTree {
	
	private static Logger LOG = Logger.getLogger(NetTree.class.getName());

	private Net root;
	
	private boolean fillGaps = true; // should we create nets that fill gaps between allocations?
	
	private Map<Net, List<Net>> map = new FastMap<Net, List<Net>>();
	
	public NetTree(Net root) {
		this.root = root;
	}
	
	public Net getRoot() { return root; }

	public Iterator<Map.Entry<Net, List<Net>>> parentChildIterator() {
		return map.entrySet().iterator();
	}

	/** returns true if the net was added */
	public boolean addNet(Net net) {
		return addNet(net, true);
	}
		
	/** returns true if the net was added */
	public boolean addNet(Net net, boolean overwrite) {
// FIXME: Terrible HACK to prevent size 1 nets since they don't draw correctly
if (net.getRange().getSize() == 1)
	return false;
		
		Net parent = net.getParent();

//		if (parent != null) { // if not the root of the internet
//			// make sure the parent is in the tree
//			Net pp = parent.getParent();
//			if (pp != null) {
//				if (!map.containsKey(pp)) {
//					addNet(parent);
//				}
//			}
//		}
		
		if (root == null) {
			root = net;
			return true;
		}
		
		List<Net> children = map.get(parent);
		
		if (children != null) {
			// before we add the net to the list of children,
			// check for any of the children that it should be the parent of, or replace
//			AddressRange range = net.getRange();
//			boolean addToChildren = true;
//			Net newParent = null;
			int insertionPt = Collections.binarySearch(children, net);
			if (insertionPt >= 0) {
			// LOG.info("Adding net with identical range to tree. Replacing " + n + " with " + net);
				if (overwrite) {
					children.set(insertionPt, net);
					return true;
				}
				return false;
			} else {
				insertionPt = (-insertionPt) - 1;
				
				boolean done = false;
				
				boolean addedNet = false;
				
				if (insertionPt > 0) {
					Net prev = children.get(insertionPt-1);
					
					if (net.getRange().contains(prev.getRange())) {
						List<Net> nc = new FastTable<Net>();
						map.put(net, nc);
						nc.add(prev);
						prev.setParent(net);
						insertionPt--; // we're replacing the prev, so move back one
						children.set(insertionPt, net);
						addedNet = true;
					} else if (prev.getRange().contains(net.getRange())) {
						net.setParent(prev);
						addNet(net);
						done = true;
						addedNet = true;
					}
				}
				if (!done && insertionPt < children.size()) {
					if (addedNet) // if we added the net, then the insertionPt points to the net now, if not it points to the one we need to check
						insertionPt++;
					
					Net next;

					if (!addedNet && insertionPt < children.size() && ((next = children.get(insertionPt)).getRange().contains(net.getRange()))) {
						net.setParent(next);
						addNet(net);
						addedNet = true;
						done = true;
					} else {
					
						while (insertionPt < children.size() && net.getRange().contains((next = children.get(insertionPt)).getRange())) {
							List<Net> nc = map.get(net);
							if (nc == null) {
								nc = new FastTable<Net>();
								map.put(net, nc);
							}
							next.setParent(net);
							nc.add(next);
							if (addedNet) {
								children.remove(insertionPt);
							} else {
								children.set(insertionPt, net);
								addedNet = true;
								insertionPt++;
							}
						}
					}
					
				}
				
				if (!addedNet) {
					children.add(insertionPt, net);
				}
			}
			int pt = Collections.binarySearch(children, net);
			if ((pt > 0 && net.compareTo(children.get(pt-1)) < 0) || (pt > 0 && pt < (children.size()-1) && net.compareTo(children.get(pt+1)) > 0))
				throw new RuntimeException("Something wrong adding net: " + net);
		
		} else {
			children = new FastTable<Net>();
			map.put(parent, children);
			children.add(net);
		}
		return true;
	}

	
	public void addNets(Net[] children) {
		for (Net n : children)
			addNet(n);
	}
	
	public void setNoChildren(Net parent) {
		if (map.get(parent) == null)
			map.put(parent, new ArrayList<Net>(0));		
	}
	
	public Net[] getSubnets(Net parent) {
		List<Net> children = map.get(parent);
		if (children == null)
			return null;
		if (fillGaps) {
			if (children.size() == 0)
				return new Net[0];
			List<Net> filled = new FastTable<Net>(children.size());
			long nextMin = parent.getRange().min();
			for (int i=0; i < children.size(); i++) {
				Net c = children.get(i);
				long cmin = c.getRange().min();
				if (nextMin < cmin) {
					AddressRange gapRange = new AddressRange(new Address(nextMin), new Address(cmin-1));
					Net gap = new GapNet(parent, gapRange);
					filled.add(gap);
				}
				filled.add(c);
				nextMin = c.getRange().max()+1;
			}
			if (nextMin < parent.getRange().max()) {
				AddressRange gapRange = new AddressRange(new Address(nextMin), parent.getRange().getMaxAddress());
				Net gap = new GapNet(parent, gapRange);
				filled.add(gap);				
			}
			return filled.toArray(new Net[filled.size()]);
		} else
			return children.toArray(new Net[children.size()]);
	}

	public boolean hasSubnets(Net parent) {
		return map.get(parent) != null;
	}
	
	public boolean getFillGaps() { return fillGaps; }
	public void setFillGaps(boolean b) { fillGaps = b; }
	
	/** Will not fill gaps! */
	public Net[] getNets() {
		FastSet<Net> netSet = new FastSet<Net>();
		for (Map.Entry<Net, List<Net>> entry : map.entrySet()) {
			netSet.add(entry.getKey());
			netSet.addAll(entry.getValue());
		}
		return netSet.toArray(new Net[netSet.size()]);
	}
	
//	/** returns each parent followed by its children in order */
//	private class NetIterator implements Iterator<Net> {
//		private Net parent;
//		private List<Net> children;
//		private FastTable<Integer> indices;
//		private int index;
//		
//		public boolean hasNext() {
//			if (index < children.size()-1) {
//				return true;
//			}
//			if ()
//			Net pp = parent.getParent();
//			
//			
//		}
//
//		public Net next() {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		public void remove() {
//		}
//		
//	}

	
}
