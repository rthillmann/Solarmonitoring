/**
 * SolarMonitoring:
 * Getting and storing TotalYield per day for Hoymiles Single-phase Microinverter connected via OpenDTU-server.
 * Data is retrieved for the Microinverter and two separate modules using OpenDTU-Web-API.
 *
 * <p>
 * the program uses three loggers
 * - YieldDayLogger - writes every night at 23:55 local standard time the daily yield to logfile "log/solar_yieldday.log"
 * - PowerLogger    - writes every minute the actual power generated  to logfile "log/solar_power.log"
 *                    the log uses a RollingFileAppender so that the logfile from the previous day is renamed as "log/solar_power.yyyy-mm-dd.log"
 * - applicationLogger  - if console-logging is enabled with cmdline-parm the actual power generated is also writen to applicationLogger
 * <p>
 * Tested with:
 * - Hoymiles HM-800 Microinverter 
 * - OpenDTU Firmware-Version v24.4.24
 * <p>
 * @author:     R. Thillmann (www.rthillmann.de)
 * @see        <a href="https://github.com/rthillmann ToDo"> github-repository </a>
 * @see        <a href="https://github.com/tbnobody/OpenDTU/blob/master/docs/Web-API.md">OpenDTU Web API</a>
 * @see        <a href="https://wib-dtu.eu/opendtu-web-schnittstelle/">OpenDTU – WEB-Schnittstelle</a>
 * 
 * @changes
 * 	2023-08-26	created
 *  2024-02-16	new openDTU-firmware version changed web-api:
 *     .../api/livedata/status does not deliver data from modules, so processing of delivered JSON-data had to be changed and another call of
 *     .../api/livedata/status?inv={serialnumber} is necessary (with serialnumber retrieved from status-call) to get module-data
 * <p>
 *  2024-02-17	v0.0.2 - date and time stamps are now delivered from logger
 *  2024-04-11	v0.0.3 - scheduling of yieldday-logging changed to UTC, because of running 1 hour to late at local summertime
 *  2024-05-05  v0.0.4 - move YieldData and YieldDataSingle to own files, created test-cases YieldData and YieldDataSingle
 *  2024-05-25  v0.0.5 - Added cmdline-parms for console-logging and logging-interval, created record CmdLineParms for holding cmdline-parms
 *                       Added tests for CmdLineParms-record
 *  2024-06-03  v0.0.6 - Modified to use 3 separate Loggers
 *  2024-07-09  v0.0.7 - Removed additional cmdline-parms again - logging to console is controlled via the use of ConsoleAppender in "logback.xml"
 *                          YieldDayLogger    - logs yield of the day once a night
 *                          PowerLogger       - logs actual generated power every minute
 *                             got additional <appender-ref ref="consoleAppender"/>
 *                          ApplicationLogger - logs application information
 *                             got additional <appender-ref ref="applicationAppender"/>
 *                                            <appender-ref ref="consoleAppender"/>
 * <p>
 * ToDo:
 * - Join various log-files to one continues log for YieldDay
 * - Optimize retrieving data from OpenDTU
 * - push final version to github.com
 */
package de.rthillmann.solarmonitoring;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.time.*;
import java.time.zone.ZoneRules;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;



public class SolarMonitoring implements Runnable {
	
	private static final String application = "SolarMonitoring     ";
	private static final String version     = "1.0.0               ";
	private static final String copyright   = "2024 by R. Thillmann";

	private static final String LOG_DIR = "log";

	private static final Logger applicationLogger = LoggerFactory.getLogger("ApplicationLogger");
	private static final Logger powerLogger = LoggerFactory.getLogger("PowerLogger");
	private static final Logger yieldDayLogger = LoggerFactory.getLogger("YieldDayLogger");

	private static final String LIVEDATA_STATUS = "/api/livedata/status";
	private static final String LIVEDATA_STATUS_INV = "/api/livedata/status?inv=";

	private volatile YieldData yieldData;

	private String openDTUServer;



	public SolarMonitoring(String[] args) {

		printProgramStart();
		openDTUServer = parseCommandline(args);


		// create log-directory if not exists
		File newDirectory = new File(LOG_DIR);
		if (! newDirectory.exists()) {
			applicationLogger.info("log-directory does not exists - trying to create...");
			if (newDirectory.mkdir()) {
				applicationLogger.info("log-directory has been created!");
			} else {
				applicationLogger.info("log-directory could not be created!");
			}
		}


		openDTUServer = "http://" + openDTUServer;


		// Try to get new data from OpenDTU-Server periodically
		// executor calls run() method all 60 seconds
		ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);
		try {
			executor.scheduleAtFixedRate(this, 0, 60, TimeUnit.SECONDS);

		} catch (Exception e) {
			applicationLogger.error("Trying to get yieldData from OpenDTU server failed!");
		}



		// start executor for writing yieldPower to log file every minute
		try {
			executor.scheduleAtFixedRate(this::yieldPower2Log, 30, 60, TimeUnit.SECONDS);

		} catch (Exception e) {
			applicationLogger.error("yieldPower2Log-Executor:");
			applicationLogger.error("Exception: " + e);
			// ToDo: maybe automatic try to restart
		}



		// start executor for writing yieldDay to log file at 23:55
		try {

			executor.scheduleAtFixedRate(
					this::yieldDay2Log,
					getDelayToMidnightInStandardtime(ZonedDateTime.now(),10),
					TimeUnit.DAYS.toMillis( 1 ) ,    // Amount of time between subsequent executions of our Runnable. Use self-documenting code rather than a “magic number” such as `86400000`.
					TimeUnit.MILLISECONDS                    // Specify the granularity of time used in previous pair of arguments.
			);	                                              // Returns a `ScheduledFuture` which you may want to cache.


		} catch (Exception e) {
			applicationLogger.error("yieldDay2Log-Executor:");
			applicationLogger.error("Exception: " + e);
			// ToDo: maybe automatic try to restart
		}

	}



	/**
	 * Calculate the initial delay for the executor to start given minutes before midnight.
	 * The sun does not react to daylight savings time, so the local time is used for calculation.
	 * This may be a bit pettifoggery, but I like it in this way.
	 *
	 * @param now the actual time in this timezone
	 * @param minutesBefore minutes to start before midnight
	 * @return the initial delay time to use as parameter in executor.scheduleAtFixedRate(...)
	 */
	private long getDelayToMidnightInStandardtime(ZonedDateTime now, long minutesBefore) {

		ZoneId z = now.getZone();					// e.g.: Europe/Berlin
		ZoneRules zoneRules = z.getRules();			// e.g.: ZoneRules[currentStandardOffset=+01:00]
		// delivers current time-zone with standard offset

		ZoneOffset standardOffset = zoneRules.getStandardOffset(Instant.now());

		ZonedDateTime nowWithoutDST = ZonedDateTime.now(standardOffset);

		ZonedDateTime shortBeforeMidnightWithoutDST = nowWithoutDST.toLocalDate().plusDays( 1 ).atStartOfDay( standardOffset ).minusMinutes(minutesBefore);  // Determine the first moment of tomorrow in our target time zone (UTC). Used as the exclusive end of our Half-Open span of time.

		long initialDelay = Duration.between(now, shortBeforeMidnightWithoutDST).toMillis();

		applicationLogger.debug("Calculating start time short before midnight (in local standard time)");
		applicationLogger.debug("=====================================================================");

		applicationLogger.debug("now                                 : " + now);							// e.g.:	2024-04-13T14:46:35.113359+02:00[Europe/Berlin]
		applicationLogger.debug("now in local standard time          : " + nowWithoutDST);					// e.g.:	2024-04-13T13:46:35.113407+01:00
		applicationLogger.debug("minutes before midnight to use      : " + minutesBefore);
		applicationLogger.debug("before midnight (using given offset): " + shortBeforeMidnightWithoutDST);	// e.g.:	2024-04-13T23:55+01:00
		applicationLogger.debug("initialDelay (Milliseconds)         : " + initialDelay);
		applicationLogger.debug("executing at local time             : " + now.plusSeconds(initialDelay/1000));

		return initialDelay;

	}



	/**
	 * print some program start information to command line.
	 */
	private void printProgramStart() {

		applicationLogger.info("---------------------------------------");
		applicationLogger.info("-   Program:   " + application + "   -");
		applicationLogger.info("-   Version:   " + version + "   -");
		applicationLogger.info("-   Copyright: " + copyright + "   -");
		applicationLogger.info("---------------------------------------");

	}


	/**
	 * Parse command line and set server-var to parameter from command-line.
	 * If no parameter is specified, print usage-messgae to command-line.
	 *
	 * @param args command-line aprms
	 * @return return server-address
	 */
	public static String parseCommandline(String[] args){

		String server = "localhost";

		if (args.length > 0) {

			// Print statements
			applicationLogger.info("The command line arguments are:");

			// Iterating the args array
			// using for each loop

			for (String val : args) {
				// Printing command line arguments
				applicationLogger.info(val);

				server = val;
				applicationLogger.info("Using following address to connect to OpenDTU-server: " + server);
			}

		}
		else {
			// Print info how to start program
			System.out.println("Usage: java -jar SolarMonitoring {url}");
			System.out.println("       url - You have to specify an URL to the OpenDTU-server.");
			System.out.println();
			System.out.println("       Example: java -jar SolarMonitoring 192.168.1.1");
			System.out.println();
			System.exit(0);
		}

		return server;
	}



	/**
	 * Try to retrieve yield data from OpenDTU server
	 * The retrieved data in Json-format will pe parsed and record of type YieldData will be created and returned.
	 *
	 * @return a record with the actual yield data or null, if no data can be retrieved.
	 */
	private YieldData determineYieldData() {

		JsonElement root = getJsonFromUrl(openDTUServer + LIVEDATA_STATUS);

		if (root == null) return null;

		if (root.isJsonObject()) {

			JsonObject rootobj = root.getAsJsonObject(); // Maybe an array, may be an object.

			// First get Yieldata from "total"-object
			JsonObject total = rootobj.getAsJsonObject("total");

			YieldDataSingle yieldDataSingleTotal = getYieldDataSingleJsonObject(total);

			JsonElement inverters = rootobj.getAsJsonArray("inverters").get(0);
			String serialNumber = inverters.getAsJsonObject().get("serial").getAsString();

			// For module data we need to use a new URL
			try {
				JsonElement moduleRoot = getJsonFromUrl(openDTUServer + LIVEDATA_STATUS_INV + serialNumber);

                assert moduleRoot != null;
                JsonObject moduleRootobj = moduleRoot.getAsJsonObject();

				JsonObject dcJsonObject = moduleRootobj.getAsJsonArray("inverters").get(0).getAsJsonObject()
						.getAsJsonObject("DC");

				JsonObject module0 = dcJsonObject.getAsJsonObject("0");
				JsonObject module1 = dcJsonObject.getAsJsonObject("1");

				YieldDataSingle yieldDataSingleModule0 = getYieldDataSingleJsonObject(module0);
				YieldDataSingle yieldDataSingleModule1 = getYieldDataSingleJsonObject(module1);

				ZonedDateTime nowWithoutDST = ZonedDateTime.now(
						ZonedDateTime.now().getZone().getRules().getStandardOffset(
								Instant.now()
						)

				);

				return new YieldData(nowWithoutDST, yieldDataSingleTotal, yieldDataSingleModule0, yieldDataSingleModule1);

			} catch (NullPointerException ex) {
				return null;
			}
		}

		return null;

	}



	/**
	 * Try to get yield-data from OpenDTU-server in Json-format.
	 *
	 * @param sURL OpenDTU-server address
	 * @return the retrieved yield-data in Json-format
	 */
	private JsonElement getJsonFromUrl(String sURL) {

		try {
			// Connect to the URL using java's native library
			URL url = new URI(sURL).toURL();

			//	Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("192.168.178.3", 80));
			//	HttpURLConnection request = (HttpURLConnection) url.openConnection(proxy);

			HttpURLConnection request = (HttpURLConnection) url.openConnection();
			//URLConnection request = url.openConnection();
			request.setConnectTimeout(0);
			request.connect();

			BufferedReader in = new BufferedReader(new InputStreamReader((InputStream) request.getContent()));
			JsonElement parsedInput = JsonParser.parseReader(in);
			in.close();

			return parsedInput;

		} catch (Exception ex) {

			applicationLogger.info(ex.getMessage());
			applicationLogger.info("Exception catched - should continue running...");

		}
		return null;

	}



	/**
	 * Parse Json-data from one Module and return a record of type YieldDataSingle.
	 * Helper-method used from method determineYieldData().
	 *
	 * @param dc the Json-data for one Module retrieved from OpenDTU-server
	 * @return a record with the actual yield data for the given module.
	 */
	private YieldDataSingle getYieldDataSingleJsonObject(JsonObject dc){


//		String name = dc.getAsJsonObject("name").get("u").getAsString();
		int yieldDayValue = dc.getAsJsonObject("YieldDay").get("v").getAsInt();
		String yieldDayUnit  = dc.getAsJsonObject("YieldDay").get("u").getAsString();
		int yieldDayDecimals = dc.getAsJsonObject("YieldDay").get("d").getAsInt();

		float yieldTotalValue = dc.getAsJsonObject("YieldTotal").get("v").getAsFloat();
		String yieldTotalUnit  = dc.getAsJsonObject("YieldTotal").get("u").getAsString();
		int yieldTotalDecimals = dc.getAsJsonObject("YieldTotal").get("d").getAsInt();

		float yieldPowerValue = dc.getAsJsonObject("Power").get("v").getAsFloat();
		String yieldPowerUnit  = dc.getAsJsonObject("Power").get("u").getAsString();
		int yieldPowerDecimals = dc.getAsJsonObject("Power").get("d").getAsInt();

		return new YieldDataSingle( yieldTotalValue, yieldTotalUnit, yieldTotalDecimals,
				yieldDayValue, yieldDayUnit, yieldDayDecimals,
				yieldPowerValue, yieldPowerUnit, yieldPowerDecimals);

	}



	/**
	 * write yieldDay to logger
	 */
	private void yieldDay2Log() {

		if ( yieldData != null) {

			// ToDo:  String yieldDataStr = yieldData.toStringReduced();
            String yieldDataStr = yieldData.toStringReduced();

			yieldDayLogger.info(yieldDataStr);
        	// System.out.println("Test-Output: " + yieldDataStr);
		}

	}


	/**
	 * write actual yieldPower to logger
	 */
	private void yieldPower2Log() {

		if ( yieldData != null) {

			powerLogger.info(yieldData.toString());

		}

	}




	// Class that implements the Runnable interface
	/*
	 * Try to get new yieldData
	 *
	 * 	method is called periodically from executor.scheduleAtFixedRate(this, 0, 60, TimeUnit.SECONDS);
	 */
	public void run()
	{
		YieldData yieldDataNew = determineYieldData();

		if ( yieldDataNew != null) {

			yieldData = yieldDataNew;

		}
	}



	public static void main(String[] args) {

		new SolarMonitoring(args);

    }
}

