/*
 * Created on 26-Jan-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Logger;

import com.liminal.ipspace.data.Address;
import com.liminal.ipspace.data.AddressRange;

public class RIRDelegationReader {

	private static Logger LOG = Logger.getLogger(RIRDelegationReader.class.getName());
	
	private boolean eof = false; // flag to indicate we've reached the end of file
	
	private URL url;
	
	public RIRDelegationReader(URL url) {
		this.url = url;
	}
	
	public AddressRange[] readIPv4Nets() throws IOException {
		LOG.info("Reading " + url);
		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
		
		ArrayList<AddressRange> ranges = new ArrayList<AddressRange>();
		while (!eof) {
			AddressRange range = readLine(reader);
			if (range != null)
				ranges.add(range);
		}
		
		LOG.info("Done reading " + url);
		
		return ranges.toArray(new AddressRange[ranges.size()]);
	}
	
	private StringBuilder buf = new StringBuilder();
	
	private AddressRange readLine(Reader reader) throws IOException {
		// format of each line of interest:
		// registry|XX|ipv4|1.2.3.4|range|date|type
		buf.delete(0, buf.length());
		
		Address startAddress = null;
		int ci;
		int numSeps = 0; // # '|' read so far
		while ((ci = reader.read()) != -1) {
			if (ci == '|') {
				numSeps++;
				if (numSeps == 2) {
					if (buf.indexOf("*") == 0) {
						skipRestOfLine(reader);
						return null; // only header lines have '*' after the first separator
					}
				} else if (numSeps == 3) { // just read in type
					if (buf.indexOf("ipv4") < 0) {
						skipRestOfLine(reader);
						return null;
					}
				} else if (numSeps == 4) { // just read in the start address
					startAddress = new Address(buf.toString());
				} else if (numSeps == 5) { // just read in the length
					int len = Integer.parseInt(buf.toString());
					Address endAddress = new Address(startAddress.getAddress() + len - 1);
					return new AddressRange(startAddress, endAddress);
				} else if (numSeps >= 6) {
					skipRestOfLine(reader);
					return null;
				}
				buf.delete(0, buf.length());
			} else if (ci == '#' && buf.length() == 0) {
				skipRestOfLine(reader); // comment line
			} else if (ci == '\n') {
				return null; // safety check to not read past the end of the line
			} else {
				buf.append((char)ci);
			}
		}
		if (ci == -1)
			eof = true;

		return null;
	}

	private void skipRestOfLine(Reader reader) throws IOException {
		int ci;
		while ((ci = reader.read()) != -1 && ci != '\n');
	}
}
