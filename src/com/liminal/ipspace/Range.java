package com.liminal.ipspace;

/**
 * @author bcort
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Range implements Comparable<Range> {

	private long min; // inclusive
	private long max; // inclusive
	
	public Range(long min, long max) {
		this.min = min;
		this.max = max;
	}

	public long min() { return min; }
	public long max() { return max; }
	
	public long getSize() { return max - min + 1; }
	
	/** If intersects returns 0 */
	public int compareTo(Range r) {
		return compareTo(r.min, r.max);
	}
	
	public int compareTo(long min, long max) {
		if (this.min < min) return -1;
		if (this.min > min) return 1;
		if (this.max < max) return -1;
		if (this.max > max) return 1;
		return 0;
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof Range))
			return false;
		Range other = (Range) o;
		return min == other.min && max == other.max;
	}
	
	public boolean intersects(Range r) {
		return intersects(r.min, r.max);
	}
	
	public boolean intersects(long min, long max) {
		if (this.min <= max && this.max >= min) return true;
		return false;
	}
	
	public boolean contains(Range r) {
		return contains(r.min, r.max);
	}
	
	public boolean contains(long min, long max) {
		return this.min <= min && this.max >= max;
	}
	
	public String toString() {
		return "" + min + " - " + max;
	}
}
