package de.rthillmann.solarmonitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YieldDataTest {

    YieldDataSingle yieldDataModule0;
    YieldDataSingle yieldDataModule1;
    YieldDataSingle yieldDataTotal;
    YieldData yieldData;

    String          resultString;
    String          resultStringReduced;


    @BeforeEach
    void setUp() {

//        ZonedDateTime now = ZonedDateTime.now();  	// e.g.: 2024-04-13T12:55:47.905436+02:00[Europe/Berlin]
//        ZoneId z = now.getZone();					// e.g.: Europe/Berlin
//        ZoneRules zoneRules = z.getRules();			// e.g.: ZoneRules[currentStandardOffset=+01:00]
//        // delivers current time-zone with standard offset
//
//        ZoneOffset standardOffset = zoneRules.getStandardOffset(Instant.now());
//
//        ZonedDateTime nowWithoutDST = ZonedDateTime.now(standardOffset);

        ZonedDateTime nowWithoutDST = ZonedDateTime.now(
                ZonedDateTime.now().getZone().getRules().getStandardOffset(
                        Instant.now()
                )
        );

        final float totalV = 157.011f;
        final String totalU = "kWh";
        final int totalD = 3;
        final int dayV = 1945;
        final String dayU = "Wh";
        final int dayD = 0;
        final float powerV = 0.4f;
        final String powerU = "W";
        final int powerD = 1;

        yieldDataModule0 = new YieldDataSingle(
                126.306f,
                "kWh",
                3,
                1030,
                "Wh",
                0,
                0.4f,
                "W",
                1);

        yieldDataModule1 = new YieldDataSingle(
                162.094f,
                "kWh",
                3,
                1032,
                "Wh",
                0,
                0.4f,
                "W",
                1);

        yieldDataTotal = new YieldDataSingle(
                288.400f,
                "kWh",
                3,
                2062,
                "Wh",
                0,
                0.0f,
                "W",
                1);



        DateTimeFormatter formatterReduced = DateTimeFormatter.ofPattern("YYYY-MM-dd");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss");

        yieldData = new YieldData( nowWithoutDST, yieldDataTotal, yieldDataModule0, yieldDataModule1 );

        resultString = nowWithoutDST.format(formatter) +
        " | Total:    288,400 kWh     2062 Wh      0,0 W | DC-0:    126,306 kWh     1030 Wh      0,4 W | DC-1:    162,094 kWh     1032 Wh      0,4 W |";

        resultStringReduced = nowWithoutDST.format(formatterReduced) +
                " | Total:    288,400 kWh     2062 Wh | DC-0:    126,306 kWh     1030 Wh | DC-1:    162,094 kWh     1032 Wh |";



        System.out.println("nowWithoutDST:" + nowWithoutDST.toString() + "   " + nowWithoutDST.format(formatterReduced));


//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd - HH:mm:ss Z");
//        String formattedString = zonedDateTime.format(formatter);



    }

    @Test
    void testToString() {
        assertEquals( resultString, yieldData.toString());
    }

    @Test
    void toStringReduced() {
        assertEquals( resultStringReduced, yieldData.toStringReduced());
    }

    @Test
    void total() {
        assertEquals( yieldDataTotal, yieldData.total());
    }

    @Test
    void module0() {
        assertEquals( yieldDataModule0, yieldData.module0());
    }

    @Test
    void module1() {
        assertEquals( yieldDataModule1, yieldData.module1());
    }
}