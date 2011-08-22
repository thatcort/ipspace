/*
 * Created on 19-Mar-2006
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

import javolution.util.FastTable;

import au.com.bytecode.opencsv.CSVReader;

import com.liminal.ipspace.data.Address;
import com.liminal.ipspace.data.AddressRange;
import com.liminal.ipspace.data.Net;
import com.liminal.ipspace.data.Registry;

public class RFC997Reader {

	public static final String RESEARCH = "Research";
	public static final String DEFENSE = "Defence";
	public static final String GOVERNMENT = "Government";
	public static final String COMMERCIAL = "Commercial";
	public static final String RESERVED = "Reserved";
	public static final String UNASSIGNED = "Unassgned";
	
	
	
	private URL url;
	
	private Net internet;
	
	public RFC997Reader(Net internet, URL url) {
		this.internet = internet;
		this.url = url;
	}

	
	public Net[] readIPv4Nets() throws IOException {
		Reader reader = new BufferedReader(new InputStreamReader(url.openStream()));
		CSVReader csv = new CSVReader(reader);

		FastTable<Net> nets = new FastTable<Net>();
		
		String[] line;
		while ((line = csv.readNext()) != null) {
			Net n = parseLine(line);
			if (n != null)
				nets.add(n);
		}
		
		return nets.toArray(new Net[nets.size()]);
	}
	
	
	private Net parseLine(String[] line) {
		String type = null;
		if (line[0] != null && line[0].length() > 0) {
			switch (line[0].charAt(0)) {
				case 'R': type = RESEARCH; 		break;
				case 'G': type = GOVERNMENT; 	break;
				case 'C': type = COMMERCIAL; 	break;
				case 'D': type = DEFENSE; 		break;
			}
		}

		Address minAddress, maxAddress;
		String addStr = line[1];
		if (addStr.length() > 15 && addStr.charAt(15) == '-') {
			if (addStr.length() > 31)
				addStr = addStr.substring(0, 31);
			String minStr = addStr.substring(0, 15);
			minStr = minStr.replace('r', '0');
			minAddress = new Address(minStr);
			String maxStr = addStr.substring(16);
			maxStr = maxStr.replace("rrr", "255");
			maxAddress = new Address(maxStr);
		} else {
			if (addStr.length() > 15)
				addStr = addStr.substring(0, 15);
			String minStr = addStr.replace('r', '0');
			String maxStr = addStr.replace("rrr", "255");
			minAddress = new Address(minStr);
			maxAddress = new Address(maxStr);
		}
		AddressRange range = new AddressRange(minAddress, maxAddress);
		
		String name = line[2];
		// get rid of the '   [reference]' part
		int bracketInd = name.lastIndexOf('[');
		if (bracketInd >= 0) {
			name = name.substring(0, bracketInd).trim();
		}
		
		// handle the unassigned and reserved nets specially
		if (name.startsWith("Reserved") || name.startsWith("Multicast"))
			type = RESERVED;
		else if (name.equalsIgnoreCase("Unassigned"))
			type = UNASSIGNED;

		return new Net(internet, range, Registry.IANA, name); // TODO: capture the type of net using the net info
	}
	
}
