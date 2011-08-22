/*
 * Created on 16-Jan-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import processing.core.PApplet;
import processing.core.PGraphics3D;
import processing.pdf.PGraphicsPDF;

import com.liminal.ipspace.data.DataManager;
import com.liminal.ipspace.whois.WhoisManager;
import com.liminal.ipspace.whois.WhoisManager.Source;
import com.liminal.p5.util.Camera;
import com.liminal.p5.util.PickState;


public class IPSpace extends PApplet {

	private static IPSpace instance;

	public static IPSpace getInstance() {
		return instance;
	}
	
	public static enum LayoutFunction {
		HILBERT,
		SIERPINSKI_KNOPP
	}
	
	private static LayoutFunction layoutFunction = LayoutFunction.HILBERT;
//	private static LayoutFunction layoutFunction = LayoutFunction.SIERPINSKI_KNOPP;
	
	public static LayoutFunction getLayoutFunction() { return layoutFunction; }
	
	public static enum Mode {
		RFC997, // the class a/b/c/d/e networks from rfc 997
		HISTORICAL,
		CURRENT
	}
	
//	private static Mode mode = Mode.RFC997;
//	private static Mode mode = Mode.HISTORICAL;
	private static Mode mode = Mode.CURRENT;
	
	public static Mode getMode() { return mode; }
	static void setMode(Mode m) { mode = m; }
	
	/** max time to use for getting historical data */
//	static String historicalMaxDate = "20020101"; // yyyymmdd e.g. 20050204
//	static String historicalMaxDate = "20030101"; // yyyymmdd e.g. 20050204
//	static String historicalMaxDate = "20040101"; // yyyymmdd e.g. 20050204
//	static String historicalMaxDate = "20050101"; // yyyymmdd e.g. 20050204
//	static String historicalMaxDate = "20060101"; // yyyymmdd e.g. 20050204
//	static String historicalMaxDate = "20070101"; // yyyymmdd e.g. 20050204
	static String historicalMaxDate = "20110101"; // yyyymmdd e.g. 20050204
	
	public static String getHistoricalMaxDate() { return historicalMaxDate; }
	
//	public static Set<WhoisManager.Source> VALID_SOURCES = WhoisManager.LOCAL_ONLY_SOURCE;
	public static Set<WhoisManager.Source> VALID_SOURCES = WhoisManager.BOTH_SOURCES;
	
	protected int minNetLevel = 0;
	
	public void setMinNetLevel(int mnl) {
		minNetLevel = mnl;
	}
	public int getMinNetLevel() { return minNetLevel; }
	
	private static int connectionTimeout = 0; // 0=infinite timeout (none), otherwise time in milliseconds
	
	public static int getConnectionTimeout() { return connectionTimeout; }
	
	private static HashMap<String, String> commandLineArgs = new HashMap<String, String>();
	/** Note that all arguments are lowercase and exclude the preceding dash */
	public static String getCommandLineArgValue(String arg) {
		return commandLineArgs.get(arg.toLowerCase());
	}

	private static Level MIN_LOGGING_LEVEL = Level.INFO;
	
	private static Logger LOG; // = Logger.getLogger(IPSpace.class.getName());
	
	protected int sizeX = 800; // 1280;
	protected int sizeY = 600; // 1024;
	
	protected DataManager dataMgr;
	
	protected VNet vinternet;
	
	protected Camera camera;
	
	protected PickState pickState;
	
	protected DrawManager drawMgr;
	
	private CameraController camController;
	
	protected ColorScheme colorScheme;
	
	private boolean cameraMoved = false;
	
	private boolean doSave = false;
	private int saveCount = 0;
	protected long startTime = System.currentTimeMillis();
	private ColorScheme pdfColorScheme;
	
	private boolean usingOpenGL = false;

	private boolean enableRecording = false;
	private boolean record = false;
//	private String recordExtension = "png";
	private int recordCount = 0;
	
	public IPSpace() throws Throwable {
		this(null);
	}
	
	public IPSpace(DataManager dataMgrParam) throws Throwable {
//com.liminal.ipspace.data.Address min = new com.liminal.ipspace.data.Address(211,0,0,0);
//com.liminal.ipspace.data.Address max = new com.liminal.ipspace.data.Address(211,255,255,255);
//com.liminal.ipspace.data.AddressRange range = new com.liminal.ipspace.data.AddressRange(min, max);
//range.getCidrRanges();

		setupLogging();
		LOG = Logger.getLogger(IPSpace.class.getName());
		
		if (commandLineArgs.containsKey("minlevel")) {
			minNetLevel = Integer.parseInt(commandLineArgs.get("minlevel"));
		}
		if (commandLineArgs.containsKey("width"))
			sizeX = Integer.parseInt(commandLineArgs.get("width"));
		if (commandLineArgs.containsKey("height"))
			sizeY = Integer.parseInt(commandLineArgs.get("height"));
		if (commandLineArgs.containsKey("record"))
			enableRecording = Boolean.parseBoolean(commandLineArgs.get("record"));

		if (mode != Mode.CURRENT)
			minNetLevel = 1;
		
		LOG.info("Program started. Creating new IPSpace.");
		LOG.info("Mode: " + mode);
		LOG.info("sources: " + VALID_SOURCES);
		LOG.info("Minimum closed net level (minLevel parameter): " + minNetLevel + ".");
		LOG.info("Connection timeout value (connectionTimeout parameter): " + (connectionTimeout == 0 ? "infinity" : Integer.toString(connectionTimeout)) + " millis.");
		if (enableRecording)
			LOG.info("Recording enabled");
		
		instance = this;
		
		this.dataMgr = dataMgrParam;
		if (this.dataMgr == null)
			this.dataMgr = new DataManager();
		
		vinternet = new VNet(null, dataMgr.getInternet());
			
		drawMgr = new DrawManager(this);
		drawMgr.setMinClosedNetLevel(minNetLevel);
		
		
	}
	
	
	public VNet getVInternet() { return vinternet; }
	
	public DataManager getDataManager() { return dataMgr; }
	
	public Camera getCamera() { return camera; }
	
	public PickState getPickState() { return pickState; }
	
	public ColorScheme getColorScheme() { return colorScheme; }

	public void setup() {
		String renderer = P3D;
		if (commandLineArgs.containsKey("renderer")) {
			if (commandLineArgs.get("renderer").toLowerCase().equals("opengl")) {
				renderer = OPENGL;
				usingOpenGL = true;
			}
		}
		LOG.info("Using renderer: " + renderer);
		size(sizeX, sizeY, renderer);

//System.out.println(System.getProperty("java.library.path"));
//		size(sizeX, sizeY, OPENGL);
//		size(sizeX, sizeY, P3D);
		
		hint(DISABLE_DEPTH_TEST);
		
		float isize = HArea.getRoot().getLineDist();
		
		float isize2 = isize * .5f;
//isize2 = 0f;
		
		float dist = isize2 / tan(PI / 3f); // * 2;

		camera = new Camera(this, isize2, -isize2, -dist, isize2, isize2, 0f, 0f, 0f, 1f);
		camera.roll(PI);
		camera.feed();

		if (mode == Mode.RFC997)
			colorScheme = new Rfc997ColorScheme();
		else
			colorScheme = new BlackColorScheme();
//			colorScheme = new WhiteColorScheme();
		
		camController = new LatLonCamController(this, 4f, camera.getCameraZ());
		
		background(colorScheme.getBackgroundColor());
		
//		openNet(vinternet);
//System.out.println("</setup>");
		
//		ortho(-10, isize+10, -10, isize+10, -10, 10);
	}
	
	public void destroy() {
		LOG.info("Shutting down IPSpace.");
		dataMgr.shutdown();
		super.destroy();
	}
	
	private LayoutFunction nextLayoutFunction = null;
	
	public void draw() {
//System.out.println("<draw>");
		
		if (doSave) {
			String fileName = "ipspace_" + (layoutFunction == LayoutFunction.HILBERT ? 'H' : 'S') + minNetLevel + "_" + startTime + "_" + saveCount++ + ".pdf";
//			String fileName = "ipspace_" + new java.util.Date(startTime).toString() + "_" + saveCount++ + ".pdf";
			PGraphicsPDF pdf = (PGraphicsPDF) beginRaw(PDF, fileName);
			System.out.println("saving pdf file: " + fileName);

			pdf.strokeJoin(MITER);
			pdf.strokeWeight(.01f);
			pdf.fill(0);
			pdf.rect(0, 0, width, height);
			
			if (pdfColorScheme == null)
				pdfColorScheme = new PDFColorScheme(this);
			ColorScheme cs = colorScheme; // store the default in pdf color scheme
			colorScheme = pdfColorScheme;
			pdfColorScheme = cs;
		}

		if (nextLayoutFunction != null) {
			layoutFunction = nextLayoutFunction;
			vinternet = null;
			drawMgr = null;
			System.gc();
			vinternet = new VNet(null, dataMgr.getInternet());
			try {
				drawMgr = new DrawManager(this);
			} catch (IOException e) {
				e.printStackTrace();
			}
			drawMgr.setMinClosedNetLevel(minNetLevel);
			nextLayoutFunction = null;
		}
		
		background(colorScheme.getBackgroundColor());
		
		dataMgr.tick();
		
//		updateView();
		cameraMoved = camController.updateCamera(camera);
		camera.feed();
		
		pickState = new PickState(this, camera);
		updateNearFarClippingPlanes();
		camera.feed();
		
//float isize = HArea.getRoot().getLineDist();

//translate(width/2f, -height/2f);

//		drawAxes(100000);
		
		drawMgr.draw((PGraphics3D)g);
//		vinternet.draw(g);
//System.out.println("</draw>");
		
		if (doSave) {
			endRaw();
			doSave = false;
			
			// switch back the colorscheme
			ColorScheme cs = colorScheme;
			colorScheme = pdfColorScheme;
			pdfColorScheme = cs;
			System.out.println("Finished writing PDF file.");
		}
		if (record) {
			String fname = "record/ipspace_" + recordCount + "_####.png";
			saveFrame(fname);
		}

	}
	
	public void savePDF() {
//		doSave = true;
// I updated to the latest version of processing and haven't updated to use the new pdf api
	}

	
	/** did the camera move the last frame */
	public boolean cameraMoved() { return cameraMoved; }
	
	private void resetCamera() {
		float isize = HArea.getRoot().getLineDist();
		float isize2 = isize * .5f;
		float dist = isize2 / tan(PI / 3f);
		
		camera.setTranslation(isize2, -isize2, -dist);
		camera.lookAt(isize2, isize2, 0f);
	}
	
	public void overheadViewpoint() {
		float isize = HArea.getRoot().getLineDist();
		float isize2 = isize * .5f;
		float dist = isize2 / (camera.getFOV() * .5f);
		dist *= 1.1f;
		
		camera.setTranslation(isize2, isize2, -dist);
		camera.lookAt(isize2, isize2, 0f);
		System.out.println("overhead viewpoint");
	}
	
	public void setLayoutFunction(LayoutFunction lfunc) {
		if (lfunc != layoutFunction) {
			nextLayoutFunction = lfunc;
		}
	}

	public void keyReleased() {
		if (key == 'l' || key == 'L')
			drawMgr.setDrawNumberLine(!drawMgr.getDrawNumberLine());
//		if (key == 'r' || key == 'R')
//			drawMgr.setLineLevelZFactor(drawMgr.getLineLevelZFactor() > 1f ? 1f : 20f);		
		else if (key == 'p' || key == 'P')
			drawMgr.setDrawPoints(!drawMgr.getDrawPoints());
		else if (key == 'c' || key == 'C')
			drawMgr.setColorPoints(!drawMgr.getColorPoints());
		else if (key == 'n' || key == 'N')
			drawMgr.setDrawNets(!drawMgr.getDrawNets());
		else if (key == 't' || key == 'T')
			drawMgr.setDrawText(!drawMgr.getDrawText());
		else if (key == 's' || key == 'S')
			doSave = true;
		else if (key == 'h' || key == 'H')
			resetCamera();
		else if (key == 'f' || key == 'F')
			setLayoutFunction(layoutFunction == LayoutFunction.HILBERT ? LayoutFunction.SIERPINSKI_KNOPP : LayoutFunction.HILBERT);
		else if (key == 'o' || key == 'O')
			overheadViewpoint();
		else if ((key == 'r' || key == 'R') && enableRecording) {
			record = !record;
			if (!record)
				recordCount++;
			LOG.info(record ? "Starting recording." : "Stopping recording");
		}
	}
	
	@Override
	public void keyPressed() {
		if (key == ESC) {
			key = 0;  // fools! don't let them escape!
		}
	}

	
	private void drawAxes(float size) {
		stroke(255f, 0f, 0f);
		pushMatrix();
		translate(size/4f, 0f, 0f);
		box(size/2f, 1f, 1f);
		popMatrix();
		stroke(0f, 255f, 0f);
		pushMatrix();
		translate(0f, size/4f, 0f);
		box(1f, size/2f, 1f);
		popMatrix();
		stroke(0f, 0f, 255f);
		pushMatrix();
		translate(0f, 0f, size/4f);
		box(1f, 1f, size/2f);
		popMatrix();
	}
	
	private void updateNearFarClippingPlanes() {
		
//float[] pk = pickState.pick(2, 0f, width * .5f, height * .5f);
//System.out.println("shotlength: " + camera.getShotLength() + "  pickDist: " + Math.sqrt(pickState.getDistance2(pk)));


		// for each corner of the vinternet, find the distance from the camera:
		float maxInternetCornerDist2 = Float.NEGATIVE_INFINITY;
//		float minInternetCornerDist2 = Float.POSITIVE_INFINITY;
		final float cx = camera.getCameraX();
		final float cy = camera.getCameraY();
		final float cz = camera.getCameraZ();
		final float iLength = vinternet.getAreas()[0].getLineDist();
		
		for (int i=0; i < 4; i++) {
			float px = (i==0 || i==3 ? 0f : iLength);
			float py = (i < 2  ? 0f : iLength);
			float pz = 0f;
			
			float dx = cx - px;
			float dy = cy - py;
			float dz = cz - pz;
		
			float dist2 = dx*dx + dy*dy + dz*dz;
			if (dist2 > maxInternetCornerDist2)
				maxInternetCornerDist2 = dist2;
//			if (dist2 < minInternetCornerDist2)
//				minInternetCornerDist2 = dist2;
		}

		// for each corner of the screen, find the distance from the camera
		float maxScreenCornerDist2 = Float.NEGATIVE_INFINITY;
//		float minScreenCornerDist2 = Float.POSITIVE_INFINITY;
		for (int i=0; i < 4; i++) {
			float sx = (i==0 || i==3 ? 0f : screen.width);
			float sy = (i < 2 ? 0f : screen.height);
			
			float[] pick = pickState.pick(2, 0f, sx, sy);
			float dist2 = pickState.getDistance2(pick);
			if (dist2 > maxScreenCornerDist2)
				maxScreenCornerDist2 = dist2;
//			if (dist2 < minScreenCornerDist2)
//				minScreenCornerDist2 = dist2;
		}
		
		// Choose the minimum distance from the maximums, since that is the maximum that can be seen
		float maxDist = (maxInternetCornerDist2 < maxScreenCornerDist2 ? maxInternetCornerDist2 : maxScreenCornerDist2);
		maxDist = (float) Math.sqrt(maxDist);
	

		float[] bottomPick = pickState.pick(2, 0f, width * .5f, height);
		float bottomDist = pickState.getDistance2(bottomPick);
		bottomDist = (float) Math.sqrt(bottomDist);
		bottomDist = bottomDist * (float) Math.cos(camera.getFOV() * .5);

		float[] centrePick = pickState.pick(2, 0f, width * .5f, height * .5f);
		float centreDist = pickState.getDistance2(centrePick);
		centreDist = (float) Math.sqrt(centreDist);
		
		float minDist = bottomDist < centreDist ? bottomDist : centreDist;
//		minDist = (float) Math.sqrt(minDist);
		
		
////		 add some safety to be sure:
//		float dif = maxDist - minDist;
//		float safety = dif * 1f;
//		maxDist += safety;
//		minDist -= safety;
//		if (minDist < 1f)
//			minDist = 1f;

		maxDist += 100f;
		minDist -= 100f;
		maxDist *= 1.5f;
		minDist *= .7f;
		if (minDist < 1f)
			minDist = 1f;
		
//		System.out.println("shotLength: " + camera.getShotLength() + " minClipDist: " + minDist);
//		System.out.println(minDist + ", " + maxDist + ", " + Math.sqrt(centreDist) + ", " + Math.sqrt(bottomDist));
		
		camera.setNearFarClip(minDist, maxDist);
	}

	private void setupLogging() throws IOException {
		if (!online) { 
			// setup file logging
			FileHandler fileHandler = new FileHandler("log/ipspace%g.log", 10000000, 2, true);
			fileHandler.setFormatter(new SimpleFormatter());
			Logger.getLogger("").addHandler(fileHandler);
		}
		
		Handler[] handlers = Logger.getLogger( "" ).getHandlers();
		for ( int index = 0; index < handlers.length; index++ ) {
			handlers[index].setLevel(MIN_LOGGING_LEVEL);
		}
		Logger.getLogger("").setLevel(MIN_LOGGING_LEVEL);
	}
	
	public boolean isUsingOpenGL() {
		return usingOpenGL;
	}

	public static void parseCommandLineArgs(String[] args) {
		for (int i=0; i < args.length; i++) {
			String arg = args[i].toLowerCase();
			String val = null;
			int ind = args[i].indexOf(':');
			if (ind < 0)
				ind = args[i].indexOf('=');
			if (ind > 0) {
				arg = arg.substring(0, ind);
				val = args[i].substring(ind+1);
			}
			while (arg.charAt(0) == '-')
				arg = arg.substring(1);
			
			commandLineArgs.put(arg, val);			
		}

		// handle the static variables
		
		if (commandLineArgs.containsKey("sources")) {
			String val = commandLineArgs.get("sources").toUpperCase();
			boolean hasL = val.indexOf('L') >= 0;
			boolean hasR = val.indexOf('R') >= 0;
			if (hasL && hasR)
				VALID_SOURCES = WhoisManager.BOTH_SOURCES;
			else if (hasL)
				VALID_SOURCES = WhoisManager.LOCAL_ONLY_SOURCE;
			else if (hasR)
				VALID_SOURCES = WhoisManager.REMOTE_ONLY_SOURCE;
		}

		if (commandLineArgs.containsKey("connectiontimeout")) {
			String ctStr = IPSpace.getCommandLineArgValue("connectiontimeout");
			if (ctStr != null) {
				try {
					connectionTimeout = Integer.parseInt(ctStr);
				} catch (NumberFormatException nfe) {
					LOG.warning("Unable to parse connection timeout value: " + ctStr + ".  Defaulting to infinite timeout.");
				}
			}
		}
		
		if (commandLineArgs.containsKey("mode")) {
			String val = commandLineArgs.get("mode");
			if (val.equalsIgnoreCase("current"))
				setMode(Mode.CURRENT);
			else if (val.equalsIgnoreCase("historical"))
				setMode(Mode.HISTORICAL);
			if (val.equalsIgnoreCase("rfc997"))
				setMode(Mode.RFC997);
		}

		if (commandLineArgs.containsKey("maxdate")) {
			historicalMaxDate = commandLineArgs.get("maxdate");
		}
	}
	
	public static void main(String[] args) {
		parseCommandLineArgs(args);

		if (commandLineArgs.containsKey("fullscreen")) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			Dimension dim = tk.getScreenSize();
			commandLineArgs.put("width", Integer.toString(dim.width));
			commandLineArgs.put("height", Integer.toString(dim.height));
			PApplet.main(new String[] {"--present", "--hide-stop", "com.liminal.ipspace.IPSpace"});
		} else {
			PApplet.main(new String[] {"com.liminal.ipspace.IPSpace"});
		}
	}
	
}
