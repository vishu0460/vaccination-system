package com.vaccine.util;

import com.vaccine.domain.Booking;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

public class CsvExportUtil {
    private CsvExportUtil() {}

    public static String exportBookings(List<Booking> bookings) throws IOException {
        StringWriter out = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT
            .withHeader("BookingId", "User", "Email", "Drive", "SlotStart", "Status"))) {

            for (Booking b : bookings) {
                printer.printRecord(
                    b.getId(),
                    b.getUser().getFullName(),
                    b.getUser().getEmail(),
                    b.getSlot().getDrive().getTitle(),
                    b.getSlot().getStartTime(),
                    b.getStatus().name()
                );
            }
            printer.flush();
        }
        return out.toString();
    }
}
