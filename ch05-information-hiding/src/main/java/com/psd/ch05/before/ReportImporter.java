package com.psd.ch05.before;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The <strong>read phase</strong> of the temporal decomposition — the other half
 * of the leak. To turn CSV text back into {@link Customer}s this class must
 * independently re-derive every format decision {@link ReportExporter} made:
 *
 * <ul>
 *   <li>the same delimiter and date pattern (taken, again, from the caller —
 *       §5.7);</li>
 *   <li>the same column <em>order</em>, hard-coded positionally in
 *       {@link #toCustomer} as fields 0..4;</li>
 *   <li>the exact inverse of the exporter's escaping rule, re-implemented from
 *       scratch in {@link #tokenize}.</li>
 * </ul>
 *
 * <p>This is exactly the duplication the book warns about: "both classes needed
 * to understand most of the structure of [the message], and parsing code was
 * duplicated in both classes." (§5.5) Because the knowledge is shared but the
 * code is not, the two copies can silently drift — change the escaping rule in
 * one place and the round-trip keeps "succeeding" while quietly corrupting any
 * field that needed quoting.
 */
public class ReportImporter {

    private final char delimiter;
    private final DateTimeFormatter dateFormat;
    private final boolean hasHeader;

    public ReportImporter(char delimiter, DateTimeFormatter dateFormat, boolean hasHeader) {
        this.delimiter = delimiter;
        this.dateFormat = dateFormat;
        this.hasHeader = hasHeader;
    }

    public List<Customer> parse(String csv) {
        List<List<String>> records = tokenize(csv);
        List<Customer> customers = new ArrayList<>();
        int start = hasHeader && !records.isEmpty() ? 1 : 0;
        for (int i = start; i < records.size(); i++) {
            customers.add(toCustomer(records.get(i)));
        }
        return customers;
    }

    /** The column order, re-stated. The mirror image of {@code ReportExporter.export}. */
    private Customer toCustomer(List<String> fields) {
        return new Customer(
                fields.get(0),
                fields.get(1),
                LocalDate.parse(fields.get(2), dateFormat),
                Long.parseLong(fields.get(3)),
                fields.get(4));
    }

    /**
     * The inverse of {@code ReportExporter.escape}, re-implemented as a full
     * RFC-4180 tokenizer: quoted fields may contain the delimiter, line breaks,
     * and doubled quotes. This knowledge has nothing to do with "reading" as a
     * time step — it is the format decision, leaked into the read phase.
     */
    private List<List<String>> tokenize(String csv) {
        List<List<String>> records = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        int i = 0;
        while (i < csv.length()) {
            char ch = csv.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        field.append('"');   // doubled quote -> literal quote
                        i += 2;
                    } else {
                        inQuotes = false;    // closing quote
                        i++;
                    }
                } else {
                    field.append(ch);
                    i++;
                }
            } else if (ch == '"') {
                inQuotes = true;
                i++;
            } else if (ch == delimiter) {
                current.add(field.toString());
                field.setLength(0);
                i++;
            } else if (ch == '\r') {
                i++;                         // swallow CR of the CRLF terminator
            } else if (ch == '\n') {
                current.add(field.toString());
                field.setLength(0);
                records.add(current);
                current = new ArrayList<>();
                i++;
            } else {
                field.append(ch);
                i++;
            }
        }
        if (!current.isEmpty()) {
            current.add(field.toString());
            records.add(current);
        }
        return records;
    }
}
