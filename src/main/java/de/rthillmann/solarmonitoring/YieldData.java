package de.rthillmann.solarmonitoring;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Record to hold YieldDataSingle-record for total and module0 and module1
 *
 * @param zonedDateTime	Timestamp for retrieved data
 * @param total		    YieldDataSingle-record for the total inverter
 * @param module0	    YieldDataSingle-record for module0
 * @param module1       YieldDataSingle-record for module1
 */
public record YieldData(ZonedDateTime zonedDateTime, YieldDataSingle total, YieldDataSingle module0, YieldDataSingle module1) {


    @Override
    public String toString() {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss");

        return zonedDateTime.format(formatter) + " | Total: " + total.toString() + " | DC-0: " + module0.toString() + " | DC-1: " + module1.toString() + " |";

    }

    public String toStringReduced() {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd");

        return zonedDateTime.format(formatter) + " | Total: " + total.toStringReduced() + " | DC-0: " + module0.toStringReduced() + " | DC-1: " + module1.toStringReduced() + " |";

    }
}


