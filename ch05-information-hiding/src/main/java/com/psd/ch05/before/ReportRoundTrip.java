package com.psd.ch05.before;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Caller code for the leaky design. Notice what the caller is forced to know:
 * the delimiter, the date pattern, and the header flag — and it must hand the
 * <em>same</em> three to both the exporter and the importer. The format decision
 * has leaked all the way out to here (§5.7). Pass mismatched knobs to the two
 * sides and the round-trip corrupts silently.
 */
public class ReportRoundTrip {

    public static void main(String[] args) {
        // The dialect, restated by the caller — and it had better match on both sides.
        char delimiter = ',';
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
        boolean header = true;

        List<Customer> customers = List.of(
                new Customer("c-001", "Ada Lovelace", LocalDate.of(2021, 3, 4), 12_500,
                        "VIP"),
                // This notes field is the stress test: it contains a comma, a
                // double-quote, and a newline — every case the escaping rule exists
                // to handle. If the exporter's escape and the importer's tokenizer
                // ever drift, THIS is the row that quietly breaks.
                new Customer("c-002", "Grace Hopper", LocalDate.of(2019, 11, 9), 8_075,
                        "Said \"ship it\", then,\nfiled the bug report"));

        ReportExporter exporter = new ReportExporter(delimiter, dateFormat, header);
        ReportImporter importer = new ReportImporter(delimiter, dateFormat, header);

        String csv = exporter.export(customers);
        List<Customer> restored = importer.parse(csv);

        System.out.println("=== before: CSV produced by the write phase ===");
        System.out.println(csv);
        System.out.println("=== before: round-tripped back through the read phase ===");
        restored.forEach(System.out::println);
        System.out.println("round-trip preserved everything? "
                + customers.equals(restored));
    }
}
