/*
 * Created on 5-Feb-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace.whois;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.logging.Logger;

import com.liminal.ipspace.IPSpace;
import com.liminal.ipspace.data.AddressRange;
import com.liminal.ipspace.data.Registry;
import com.liminal.ipspace.whois.WhoisManager.Source;

public class RIRStatisticsHandler {

	private static Logger LOG = Logger.getLogger(RIRStatisticsHandler.class.getName());
	
//	private final int DAY_MILLIS = 1000 * 60 * 60 * 24;
	
	Set<Source> sources;
	
	private File localBaseDir;
	
	private URL remoteBaseUrl;
	
	private Calendar cal = GregorianCalendar.getInstance();
	
	private Registry registry;
	
	
	public RIRStatisticsHandler(Registry registry, Set<Source> sources) throws MalformedURLException {
		this.registry = registry;
		this.sources = sources;
		
		localBaseDir = new File("data/rirStats");
		if (!localBaseDir.exists())
			localBaseDir.mkdir();
	
		remoteBaseUrl = createRemoteBaseUrl(registry);
	}
	
	private static URL createRemoteBaseUrl(Registry reg) throws MalformedURLException {
		return new URL("ftp://" + reg.getFtpServer() + "/pub/stats/");
	}
	
	public AddressRange[] loadResults() throws IOException {
		String reg = registry.getName().toLowerCase();
		if (reg.equals("ripe"))
			reg += "ncc";
		
		File localLatest = null;
		
		if (IPSpace.getMode() != IPSpace.Mode.CURRENT) {
			localLatest = getLocalFile(reg, IPSpace.getHistoricalMaxDate());
			
			if (localLatest == null)
				return new AddressRange[0];
		} else {
			if (sources.contains(WhoisManager.Source.LOCAL))
				localLatest = getLatestLocalFile(reg);
			
			URL remoteLatest = null;
			if (sources.contains(WhoisManager.Source.REMOTE)) {
				try {
					remoteLatest = getRemoteLatestFile(reg);
				} catch (ConnectException conx) {
					Registry r = (registry == Registry.ARIN ? Registry.RIPE : Registry.ARIN);
					LOG.warning("Failed to load RIR data from " + reg + ". Attempting to load from " + r + " instead.");
					remoteBaseUrl = createRemoteBaseUrl(r);
					remoteLatest = getRemoteLatestFile(reg);
				}
			}
			
			if (remoteLatest != null) {
				String remotePath = remoteLatest.getPath();
				String remoteName = remotePath.substring(remotePath.lastIndexOf('/') + 1);
				if (localLatest == null || remoteName.compareTo(localLatest.getName()) > 0)
					localLatest = saveRemoteFile(reg, remoteLatest);
			}
		}
		
		if (localLatest == null)
			throw new IOException("Unable to find delegation file for registry: " + registry);
		
		URL localLatestUrl = localLatest.toURL();
		
		
		RIRDelegationReader rirReader = new RIRDelegationReader(localLatestUrl);
		
		return rirReader.readIPv4Nets();
	}
	
	private File saveRemoteFile(String registry, URL remoteUrl) throws IOException {
		LOG.info("Saving local copy of " + remoteUrl);
		String remotePath = remoteUrl.getPath();
		String remoteName = remotePath.substring(remotePath.lastIndexOf('/') + 1);

		// read into a tmp file
		File localDir = new File(localBaseDir, registry + '/');
		File localTmp = File.createTempFile("tmp", ".tmp", localDir);
		if (localTmp.exists())
			localTmp.delete();
		if (!localTmp.createNewFile())
			throw new IOException("Unable to create local file: " + localTmp.getAbsolutePath());
		
		File localFile = null;
		try {
		
			BufferedWriter writer = new BufferedWriter(new FileWriter(localTmp));
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(remoteUrl.openStream()));
			int ci;
			while ((ci = reader.read()) != -1) {
				writer.write((char)ci);
			}
			writer.flush();
			writer.close();
			reader.close();

			localFile = new File(localDir, remoteName);
			if (localFile.exists())
				localFile.delete();
			localTmp.renameTo(localFile);
			
		} catch (IOException iox) {
			localTmp.delete();
			throw iox;
		}
		
		return localFile;
	}
	
//	private int getDayOffset(File file) {
//		cal.setTimeInMillis(System.currentTimeMillis());
//		
//		String fname = file.getName();
//		return getDayOffset(fname);
//	}
	
//	private int getDayOffset(String fname) {
//		int len = fname.length();
//		String dayStr = fname.substring(len-2);
//		String monthStr = fname.substring(len-4, len-2);
//		String yearStr = fname.substring(len-8, len-4);
//		int day = Integer.parseInt(dayStr);
//		int month = Integer.parseInt(monthStr) - 1; // month is 0-based in Calendar
//		int year = Integer.parseInt(yearStr);
//		
//		cal.set(year, month, day);
//		long t = cal.getTimeInMillis();
//		
//		return (int)(System.currentTimeMillis() - t) / DAY_MILLIS;
//	}
	
	private URL getRemoteLatestFile(String registry) throws IOException {
		URL url = null;

		int dayOffset = 1;
		final int offsetLimit = -10; 
		while (url == null && dayOffset > offsetLimit) {
			String dateStr = createDateString(dayOffset);
			InputStream in = null;
			url = new URL(remoteBaseUrl, registry + "/delegated-" + registry + "-" + dateStr);
			URLConnection connection = url.openConnection();
			int timeout = IPSpace.getConnectionTimeout();
			connection.setConnectTimeout(timeout);
			connection.setReadTimeout(timeout);
			try {
				connection.connect();
			} catch (IOException conx) {
				LOG.severe("Unable to connect to " + url);
				throw conx;
			}
			try {
				in = connection.getInputStream();
			} catch (IOException iox) { // if it throws an exception, then the file doesn't exist, so we try an earlier date
				url = null;
				dayOffset--;
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException iox) { 
						/* ignore */
					} finally {
						in = null;
					}
				}
			}
		}
		
		if (url == null && dayOffset <= offsetLimit)
			LOG.warning("Unable to find RIR delegation file for " + registry + " less than " + offsetLimit + " days old.");
		
		return url;
	}
	
	/** returns the latest local file at least as old as the given date */
	private File getLocalFile(String registry, String maxDate) {
		File serverDir = new File(localBaseDir, registry);

		if (!serverDir.exists())
			serverDir.mkdir();

		File[] files = serverDir.listFiles(new DelegatedFilenameFileter());
		
		if (files == null || files.length == 0)
			return null;

		Arrays.sort(files, new FilenameComparator());

		if (maxDate == null)
			return files[files.length-1];

		for (int i=files.length-1; i>=0; i--) {
			String name = files[i].getName();
			String fdate = name.substring(name.length()-8);
			if (fdate.compareTo(maxDate) <= 0)
				return files[i];
		}
		
		return null;
	}
	
	/** Returns the local file with the most recent name. Will create the local directory if necessary */
	private File getLatestLocalFile(String registry) {
		return getLocalFile(registry, null);
	}
	
//	private String createFileName(String server, int dayOffset) {
//		return "delegated-" + server.toLowerCase() + "-" + createDateString(dayOffset);
//	}
	
	private String createDateString(int dayOffset) {
		cal.setTimeInMillis(System.currentTimeMillis());
		
		cal.add(Calendar.DAY_OF_YEAR, dayOffset);
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DATE);
		
		return Integer.toString(year) + (month < 10 ? "0" : "") + Integer.toString(month) + (day < 10 ? "0" : "") + Integer.toString(day);
	}

	private class DelegatedFilenameFileter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			if (!name.startsWith("delegated-") || name.length() < 20)
				return false;
			try {
				Integer.parseInt(name.substring(name.length()-8));
			} catch (NumberFormatException nfe) {
				return false;
			}
			return true;
		}
	}
	
	private class FilenameComparator implements Comparator<File> {
		public int compare(File f1, File f2) {
			return f1.getName().compareToIgnoreCase(f2.getName());
		}
	}
	
}
