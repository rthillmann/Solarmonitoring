# SolarMonitoring
In 2023 I build my balcony power plant from a set containing a Hoymiles HM-800 Microinverter and 2 solar modules.
Because I did not like to pay for connecting the inverter to a cloud-storage offered from Hoymiles, I decided
to build an OpenDTU-server described at https://blog.helmutkarger.de/balkonkraftwerk-teil-8-opendtu-und-ahoydtu-fuer-hoymiles-wechselrichter/ 
I also wanted to store some data as the daily yield and in the beginning hourly data to check if all is working fine. The OpenDTU-Webgui shows actual data,
but does not store them at the OpenDTU-server. So I decided to write this little program to retrieve data via the OpenDTU-Webapi and store them on my
computer, which is running the whole day. 

## Purpose
Get messages from e.g. the Hoymiles HM-800 Microinverter via OpenDTU Web-API and store values like Date, yieldtotal, yieldday, yieldday-module1, yieldday-module2, to log-file.

- Store yield-data with daily yield and total yield from microinverter and modules to a file
```
2024-05-29 | Total:    354,101 kWh     3105 Wh | DC-0:    159,350 kWh     1555 Wh | DC-1:    194,751 kWh     1550 Wh |
```                                              

- Store yield-data every minute with yield data from microinverter and modules to a daily file
```
2024-05-30 13:48:25 | Total:    355,034 kWh      933 Wh     51,1 W | DC-0:    159,815 kWh      465 Wh     26,7 W | DC-1:    195,219 kWh      468 Wh     27,0 W |
```

- Write yield-data to console at specified interval if wished as specified as command line parameter
```
2024-05-30 13:48:25 | Total:    355,034 kWh      933 Wh     51,1 W | DC-0:    159,815 kWh      465 Wh     26,7 W | DC-1:    195,219 kWh      468 Wh     27,0 W |
```

The timestamp written to the logfile is created when data is retrieved from OpenDTU in standard time, because sun usually ignores summer time settings.
So summer time is ignored and the timestamp in data-record is retrieved from local standard-time.

At the moment of writing we have here summer time and we are 1 hour ahead of standard time in our zone and two hours ahead of UTC:
```
ZoneId z = now.getZone();					// e.g.: Europe/Berlin
ZoneRules zoneRules = z.getRules();			// e.g.: ZoneRules[currentStandardOffset=+01:00]
ZonedDateTime now = ZonedDateTime.now();  	// e.g.: 2024-04-13T12:55:47.905436+02:00[Europe/Berlin]
```

So the time stamp for the retrieved data from OpenDTU would be:
```
2024-04-13T11:55:47.905436+01:00
```

## Background
This project was started because it seems OpenDTU does not store yieldday and yieldtotal values from microinverter to files and I wanted to have some monitoring
about the daily generated power without using the costly original Hoymiles DTU (Telemetry Gateway) with their cloud access.

## Goal
Want to have a logfile for daily yield like the following example:
```
2023-09-19 | Total:     27,391 kWh      930 Wh | DC-0:      0,000 kWh        0 Wh |  DC-1:     27,391 kWh      930 Wh
```                                              
with columns as Date, yieldtotal, yieldday, DC-0: yieldtotal, yieldday, DC-1: yieldtotal, yieldday, so that I can see the rising total yield and if both modules are working with equal power.  

## Tested with following components
* Software:
    * OpenDTU Firmware-Version v24.4.24
* Hardware:
    * Hoymiles HM-800 Microinverter
    * AzDelivery ESP32 NodeMCU Module WLAN WiFi Development Board | Dev Kit C V2
    * AzDelivery NRF24L01 2,4 GHz Wireless Module 

## Java prerequisites
As of using records the used Java version may be at least Java 16 (records were implemented first as preview since JDK14).
Java 17 has been release half a year later as a LTS-version, so Java 17 should be used as minimum version.

## Usage

Copy SolarMonitoring.jar from build/ to a directory e.g. SolarMonitoring
### Change directory to your home-directory
cd
### Create new directory SolarMonitoring
mkdir SolarMonitoring
cd SolarMonitoring

### Create new sub-directory for logfiles
mkdir log

### Start program
- MacOS: On my iMAC I have to use e.g. caffeinate to prevent it from going to some sleeping-mode in the night and therefor loosing data:

``` java 
  caffeinate java -jar SolarMonitoring {OpenDTU-server-URL}
  e.g. caffeinate java -jar SolarMonitoring 192.168.1.99
```


- Linux: Not tested yet!

## Links
https://github.com/tbnobody/OpenDTU/blob/master/docs/Web-API.md
https://wib-dtu.eu/opendtu-web-schnittstelle/

The easiest way: Use gson, google's own goto json library. https://code.google.com/p/google-gson/
