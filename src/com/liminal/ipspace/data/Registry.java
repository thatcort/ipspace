/*
 * Created on 1-Apr-07
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.data;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum Registry {

	IANA ("IANA", null, null, RegistryType.OTHER),
	ARIN ("ARIN", "whois.arin.net", "ftp.arin.net", RegistryType.RIR),
	APNIC ("APNIC", "whois.apnic.net", "ftp.apnic.net", RegistryType.RIR),
	RIPE ("RIPE", "whois.ripe.net", "ftp.ripe.net", RegistryType.RIR),
	AFRINIC ("AFRINIC", "whois.afrinic.net", "ftp.afrinic.net", RegistryType.RIR),
	LACNIC ("LACNIC", "whois.lacnic.net", "ftp.lacnic.net", RegistryType.RIR),
	JPNIC ("JPNIC", "whois.nic.ad.jp", null, RegistryType.NIR);

	private static Map<String, Registry> nameRegistryMap = new HashMap<String, Registry>(Registry.values().length * 2);
	static {
		for (Registry reg : Registry.values()) {
			nameRegistryMap.put(reg.getName(), reg);
		}
	}
	
	public static enum RegistryType { RIR, NIR, OTHER }
	
	public static Set<Registry> RIRs = Collections.unmodifiableSet(EnumSet.of(ARIN, APNIC, RIPE, AFRINIC, LACNIC));
	
	private String name;
	private String whoisServer;
	private String ftpServer;
	private RegistryType type;

	private Registry(String name, String whoisServer, String ftpServer, RegistryType type) {
		this.name = name;
		this.whoisServer = whoisServer;
		this.ftpServer = ftpServer;
	}

	public String getName() {
		return name;
	}
	
	public String getWhoisServer() {
		return whoisServer;
	}
	
	public String getFtpServer() {
		return ftpServer;
	}

	public RegistryType getType() {
		return type;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	public static Registry getRegistryByName(String name) {
		return nameRegistryMap.get(name);
	}
	
}
