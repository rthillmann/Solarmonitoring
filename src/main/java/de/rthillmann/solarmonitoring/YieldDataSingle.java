package de.rthillmann.solarmonitoring;

/**
 * Record to hold data as yieldtotal, yieldday and power with units and decimals.
 * @param totalV	value of total yield
 * @param totalU    unit of total yield
 * @param totalD	decimals of total yield
 * @param dayV      value of day yield
 * @param dayU		unit of day yield
 * @param dayD		decimals of day yield
 * @param powerV	power
 * @param powerU    unit of power
 * @param powerD	decimals of power
 */
public record YieldDataSingle(float totalV, String totalU, int totalD,
                              int dayV, String dayU, int dayD,
                              float powerV, String powerU, int powerD) {

    @Override
    public String toString() {

        return String.format("%10.3f", totalV) + " " + totalU
                + " " +  String.format("%8d", dayV) + " " +  dayU
                + " " +  String.format("%8.1f", powerV) + " " +  powerU;

    }

    public String toStringReduced() {

        return String.format("%10.3f", totalV) + " " + totalU
                + " " +  String.format("%8d", dayV) + " " +  dayU;

    }
}

