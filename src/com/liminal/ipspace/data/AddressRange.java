/*
 * Created on 2-Jan-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.data;

import java.util.ArrayList;
import java.util.List;

import com.liminal.ipspace.Range;

public class AddressRange extends Range {

	private Address minAddress, maxAddress;
	
	public AddressRange(Address min, Address max) {
		super(min.getAddress(), max.getAddress());
		
		this.minAddress = min;
		this.maxAddress = max;
	}
	
	
	public Address getMinAddress() { return minAddress; }
	public Address getMaxAddress() { return maxAddress; }
	
	public String toString() {
		if (min() == max())
			return minAddress.toString();
		return minAddress.toString() + " - " + maxAddress.toString();
	}
	
	public String[] getCidrRanges() {
		return getCidrRanges(min(), max());
	}
	
	/** Expects a string of the form 'x.x.x.x - y.y.y.y' or a CIDR block of the form '142.106/16' */
	public static AddressRange parseAddressRange(String rangeStr) {
		int slashInd = rangeStr.indexOf('/');
		if (slashInd == -1) {
			int dashInd = rangeStr.indexOf('-');
			Address minAdd = new Address(rangeStr.substring(0, dashInd).trim());
			Address maxAdd = new Address(rangeStr.substring(dashInd+1).trim());
			return new AddressRange(minAdd, maxAdd);
		} else {
			String[] octs = rangeStr.substring(0, slashInd).split("\\.");
			long addy = 0L;
			for (int i=0; i < octs.length; i++) {
				long octl = Integer.parseInt(octs[i]);
				addy += (octl << (24L - (i * 8L)));
			}

			int cidr = Integer.parseInt(rangeStr.substring(slashInd+1));
			long length = (1L << (32 - cidr));
			
			Address minAdd = new Address(addy);
			Address maxAdd = new Address(addy + length - 1L);
			return new AddressRange(minAdd, maxAdd);
		}
	}
	
	/** min and max are inclusive */
	public static String[] getCidrRanges(long min, long max) {
		ArrayList<String> cidrs = new ArrayList<String>(3);
		getCidrRanges(min, max, cidrs);
		return cidrs.toArray(new String[cidrs.size()]);
	}
	
	private static void getCidrRanges(long min, long max, List<String> cidrs) {
		// here's the algorithm:
		// 1) find the most significant differing bit between min and max
		// 2) from that bit, find the least significant '1' bit in min --> cidr mask extends to there
		// 3) set all bits to right of the mask to '1' --> this gives the max of that mask
		// 4) add 1 and repeat for the new min and the old max
		while (min <= max) {
//System.out.println("min:    " + Long.toBinaryString(min));
//System.out.println("max:    " + Long.toBinaryString(max));
			
			long xored = min ^ max;
//System.out.println("xored:  " + Long.toBinaryString(xored));
			int msdif = 0; // most significant differing bit, counted from right starting at 0
			for (int i=0; i < 63; i++) {
				long index = 1L << i;
				if ((xored & index) != 0L)
					msdif = i;
			}
			int ls1 = msdif+1; // least significant '1' counted from right in min starting at 1
			for (int i=msdif-1; i >= 0; i--) {
				long index = 1L << i;
				if ((min & index) != 0L) {
					ls1 = i+1;
				}
			}
//System.out.println("ls1: " + ls1);
			long submax = min;
			for (int i=0;i < ls1; i++) {
				submax |= (1L << i);
			}
//System.out.println("submax: " + Long.toBinaryString(submax));
			// now convert min..submax into a cidr string:
			int mask = 32 - ls1;
			String cidr = getCidrString(min, mask);
//System.out.println("cidr: " + cidr);
			cidrs.add(cidr);

			min = submax + 1;
		}
	}
	
	private static String getCidrString(long min, int mask) {
		StringBuilder buf = new StringBuilder(20);
		int oct0 = (int)((min >> 24) & 0xFFL);
		buf.append(Integer.toString(oct0));
		if (mask > 8) {
			buf.append('.');
			int oct1 = (int)((min >> 16) & 0xFFL);
			buf.append(Integer.toString(oct1));
		}
		if (mask > 16) {
			buf.append('.');
			int oct2 = (int)((min >> 8) & 0xFFL);
			buf.append(Integer.toString(oct2));			
		}
		if (mask > 24) {
			buf.append('.');
			int oct3 = (int)(min & 0xFFL);
			buf.append(Integer.toString(oct3));			
		}
		if (mask < 32) {
			buf.append('/');
			buf.append(Integer.toString(mask));
		}
		return buf.toString();
	}

}
