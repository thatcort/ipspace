package com.liminal.ipspace.data;

/**
 * @author bcort
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Address {

	private long address;
	
	public Address(long address) {
		if (address < 0 || address >= (1L << 33))
			throw new IllegalArgumentException("Address out of range: " + address +". Must be positive 32 bit #.");
		this.address = address;
	}
	
	public Address(int oct0, int oct1, int oct2, int oct3) {
		init(oct0, oct1, oct2, oct3);
	}
	
	public Address(String octets) {
		String[] octs = octets.split("\\.");
		init(Integer.parseInt(octs[0]), Integer.parseInt(octs[1]), Integer.parseInt(octs[2]), Integer.parseInt(octs[3]));
	}
	
	private void init(int oct0, int oct1, int oct2, int oct3) {
		if (oct0 < 0 || oct0 > 255)
			throw new IllegalArgumentException("Address out of range: " + oct0 + ". Must be between 0 and 255.");
		if (oct1 < 0 || oct1 > 255)
			throw new IllegalArgumentException("Address out of range: " + oct1 + ". Must be between 0 and 255.");
		if (oct2 < 0 || oct2 > 255)
			throw new IllegalArgumentException("Address out of range: " + oct2 + ". Must be between 0 and 255.");
		if (oct3 < 0 || oct3 > 255)
			throw new IllegalArgumentException("Address out of range: " + oct3 + ". Must be between 0 and 255.");
		
		this.address = (((long)oct0) << 24) | (((long)oct1) << 16) | (((long)oct2) << 8) | (long)oct3;
	}
	
	public long getAddress() { return address; }
	
	public int[] getOctets() {
		int[] ints = new int[4];
		ints[0] = ((int)(address >> 24)) & 0xFF;
		ints[1] = ((int)address >> 16) & 0xFF;
		ints[2] = ((int)address >> 8) & 0xFF;
		ints[3] = ((int)address)  & 0xFF;
		return ints;
	}
	
	public int getOctet(int index) {
		if (index < 0 || index > 3)
			throw new IllegalArgumentException("Octet index must be between 0 and 3.");
		return ((int)address >> (8 * (3-index))) & 0xFF;
	}
	
	public byte[] getBytes() {
		byte[] bytes = new byte[4];
		bytes[0] = (byte)(((int)(address >> 24)) & 0xFF);
		bytes[1] = (byte)(((int)address >> 16) & 0xFF);
		bytes[2] = (byte)(((int)address >> 8) & 0xFF);
		bytes[3] = (byte)(((int)address)  & 0xFF);
		return bytes;		
	}
	
	public String toString() {
		int[] ints = getOctets();
		return ints[0] + "." + ints[1] + "." + ints[2] + "." + ints[3];
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		Address add = (Address) obj;
		return address == add.address;
	}
	
}
