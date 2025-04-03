package de.rthillmann.solarmonitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YieldDataSingleTest {

    private final float totalV = 157.011f;
    private final String totalU = "kWh";
    private final int totalD = 3;
    private final int dayV = 1945;
    private final String dayU = "Wh";
    private final int dayD = 0;
    private final float powerV = 0.4f;
    private final String powerU = "W";
    private final int powerD = 1;

    YieldDataSingle yieldDataSingle;

    @BeforeEach
    void setUp() {

        yieldDataSingle = new YieldDataSingle( totalV, totalU, totalD, dayV, dayU, dayD, powerV, powerU, powerD);
    }



    @Test
    void testToString() {
        assertEquals("   157,011 kWh     1945 Wh      0,4 W", yieldDataSingle.toString() );
    }

    @Test
    void toStringReduced() {
        assertEquals("   157,011 kWh     1945 Wh", yieldDataSingle.toStringReduced() );
    }

    @Test
    void totalV() {
        assertEquals( totalV, yieldDataSingle.totalV());
    }

    @Test
    void totalU() {
        assertEquals( totalU, yieldDataSingle.totalU());
    }

    @Test
    void totalD() {
        assertEquals(totalD, yieldDataSingle.totalD());
    }

    @Test
    void dayV() {
        assertEquals(dayV, yieldDataSingle.dayV());
    }

    @Test
    void dayU() {
        assertEquals(dayU, yieldDataSingle.dayU());
    }

    @Test
    void dayD() {
        assertEquals(dayD, yieldDataSingle.dayD());
    }

    @Test
    void powerV() {
        assertEquals(powerV, yieldDataSingle.powerV());
    }

    @Test
    void powerU() {
        assertEquals(powerU, yieldDataSingle.powerU());
    }

    @Test
    void powerD() {
        assertEquals(powerD, yieldDataSingle.powerD());
    }
}