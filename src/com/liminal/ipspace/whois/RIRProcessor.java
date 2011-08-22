package com.liminal.ipspace.whois;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javolution.util.FastTable;

import com.liminal.ipspace.data.AddressRange;
import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.data.Registry;
import com.liminal.ipspace.whois.WhoisManager.Source;

/** This class' methods should only be called by the executor thread */
class RIRProcessor {
	
	private static Logger LOG = Logger.getLogger(RIRProcessor.class.getName());
	
	/**
	 * 
	 */
	private final WhoisManager whoisMgr;
	/**
	 * @param manager
	 */
	RIRProcessor(WhoisManager manager) {
		whoisMgr = manager;
	}

	/** Note: This method must be called by executor */
	Net[] processNonArinRIRDelegations(Registry registry, AddressRange[] ranges, Net[] roots) {
		Arrays.sort(roots, new Net.NetRangeComparator());
		Arrays.sort(ranges);
		
		FastTable<Net> nets = new FastTable<Net>(ranges.length + 5);

		int rootIndex = 0;
		int rangeStartIndex = 0;;
		int rangeLength = 0;
		while (rootIndex < roots.length && rangeStartIndex < ranges.length) {
			int rangeEndIndex = rangeStartIndex;
			Net root = roots[rootIndex];
			while (rangeEndIndex < ranges.length && root.getRange().intersects(ranges[rangeEndIndex])) {
				rangeLength++;
				rangeEndIndex++;
			}
			
			if (rangeLength > 0) {
				if (rangeLength == 1 && root.getRange().equals(ranges[rangeStartIndex])) {
					// skip it since its a root we've already loaded
				} else {
					// create subnets...
					for (int i=0; i < rangeLength; i++) {
						AddressRange range = ranges[rangeStartIndex + i];
						String id = range.getMinAddress().toString() + "-" + range.getMaxAddress().toString();
						Net net = new Net(root, range, registry, id);
						nets.add(net);
					}
				}
				rangeStartIndex += rangeLength;
			}
			
			rootIndex++;
			rangeLength = 0;
		}
		return nets.toArray(new Net[nets.size()]);
	}
	
	/**
	 * Processing of Arin's RIR delegation reports must be handled differently because of how Arin's whois database interface works.
	 * Because Arin does not allow searching their db by address ranges, we need to have the Arin whois db ID for each address range.
	 * The only way to get this information is to load it from a subnet query on the containing root net. 
	 * Note: This method must be called by executor
	 */
	Net[] processArinRIRDelegations(AddressRange[] ranges, Net[] roots, Set<Source> sources) throws Exception {
		Arrays.sort(roots, new Net.NetRangeComparator());
		Arrays.sort(ranges);
		
		FastTable<Net> nets = new FastTable<Net>(ranges.length + 5);
		
		int rootIndex = 0;
		int rangeStartIndex = 0;;
		int rangeLength = 0;
		while (rootIndex < roots.length && rangeStartIndex < ranges.length) {
			int rangeEndIndex = rangeStartIndex;
			Net root = roots[rootIndex];
			while (rangeEndIndex < ranges.length && root.getRange().intersects(ranges[rangeEndIndex])) {
				rangeLength++;
				rangeEndIndex++;
			}
			
			if (rangeLength > 0) {
				if (rangeLength == 1 && root.getRange().equals(ranges[rangeStartIndex])) {
					// skip it since its a root we've already loaded
				} else {
					// we need to load the subnets from the whois database in order to get the arin id
					AddressRange[] expected = new AddressRange[rangeLength];
					System.arraycopy(ranges, rangeStartIndex, expected, 0, rangeLength);
					Net[] rootsubs = getArinSubnets(root, expected, sources);
					if (rootsubs != null) {
						for (Net n : rootsubs) {
							nets.add(n);
						}
					} else {
						LOG.warning("Expected subnets not found for " + root);
					}
				}
				rangeStartIndex += rangeLength;
			}
			
			rootIndex++;
			rangeLength = 0;
		}
		return nets.toArray(new Net[nets.size()]);
	}
	private Net[] getArinSubnets(Net root, AddressRange[] expected, Set<Source> sources) throws Exception {
		GetSubnetResult result = whoisMgr.getSubnetsBlocking(root, sources.contains(Source.LOCAL) ? Source.LOCAL : Source.REMOTE);
//		NetRequestResult nrr = whoisMgr.registryClientMap.get(WhoisManager.ARIN).getSubNets(root, sources);
//		if (nrr.responseSource == Source.LOCAL) {
		if (result.getResponseSource() == Source.LOCAL && sources.contains(Source.LOCAL)) { // the second check that source.local was requested is because lacnic doesn't provide subnets, so the requests always return a source of local, so this avoids infinite loops 
			if (result.getSubnets() == null)
				return getArinSubnets(root, expected, WhoisManager.REMOTE_ONLY_SOURCE);
			
			// process it to ensure it contains all the nets in expected (it is ok if it contains more, since sometimes arin lists a net that is assigned to another reg)
			TreeSet<AddressRange> netset = new TreeSet<AddressRange>();
			for (Net n : result.getSubnets()) {
				netset.add(n.getRange());
			}
			for (AddressRange r : expected) {
				if (!netset.contains(r) && sources.contains(Source.REMOTE)) {
					return getArinSubnets(root, expected, WhoisManager.REMOTE_ONLY_SOURCE);
				}
			}
		} else {

//			whoisMgr.db.logSearch(nrr.request, nrr.response);
		}
		return result.getSubnets();
	}
}