package com.psd.ch05.before;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringJoiner;

/**
 * The <strong>write phase</strong> of a temporal decomposition. The application
 * was carved up by <em>when</em> things happen — "first we export the report,
 * later something imports it" — so writing lives in one class and reading lives
 * in another. "In temporal decomposition, the structure of a system corresponds
 * to the time order in which operations will occur." (§5.3)
 *
 * <p>The trap is that the CSV <em>format</em> is not a time step; it is a single
 * design decision that manifests at <em>both</em> times. This class therefore
 * has to know the entire dialect: the column set and their order, the RFC-4180
 * escaping rule, the date pattern, money-as-minor-units, and the header names.
 * {@link ReportImporter} has to know all of it too. "Suppose two classes both
 * have knowledge of a particular file format (perhaps one class reads files in
 * that format and the other class writes them)... they both depend on the file
 * format: if the format changes, both classes will need to be modified."
 * (§5.2) None of it shows up in either interface, which is what makes it the
 * <em>back-door</em> leakage the book calls "more pernicious... because it isn't
 * obvious." (§5.2)
 *
 * <p>On top of that, the dialect knobs are pushed onto the caller through the
 * constructor (delimiter, date pattern, whether to write a header). That is the
 * defaults mistake from §5.7: "if the caller does specify a value, it probably
 * results in information leakage between the [library] and the caller." The
 * format decision now lives in <em>three</em> places — exporter, importer, and
 * the calling code that must hand both the same knobs.
 */
public class ReportExporter {

    /** Column order — duplicated, by hand, in {@link ReportImporter#parse}. */
    private static final String HEADER = "id,name,signup_date,balance_minor,notes";

    private final char delimiter;
    private final DateTimeFormatter dateFormat;
    private final boolean writeHeader;

    public ReportExporter(char delimiter, DateTimeFormatter dateFormat, boolean writeHeader) {
        this.delimiter = delimiter;
        this.dateFormat = dateFormat;
        this.writeHeader = writeHeader;
    }

    public String export(List<Customer> customers) {
        StringBuilder out = new StringBuilder();
        if (writeHeader) {
            out.append(HEADER).append("\r\n");
        }
        for (Customer c : customers) {
            // Column order is encoded right here, positionally. ReportImporter
            // re-encodes the very same order when it reads fields back out.
            StringJoiner row = new StringJoiner(String.valueOf(delimiter));
            row.add(escape(c.id()));
            row.add(escape(c.name()));
            row.add(escape(c.signupDate().format(dateFormat)));
            row.add(escape(Long.toString(c.balanceMinor())));
            row.add(escape(c.notes()));
            out.append(row).append("\r\n");
        }
        return out.toString();
    }

    /**
     * Half of the escaping decision. A field that contains the delimiter, a
     * quote, or a line break is wrapped in double-quotes and its own quotes are
     * doubled. {@link ReportImporter} carries the exact inverse of this rule in a
     * completely separate tokenizer; nothing but discipline keeps the two in
     * step.
     */
    private String escape(String field) {
        boolean mustQuote =
                field.indexOf(delimiter) >= 0
                        || field.indexOf('"') >= 0
                        || field.indexOf('\n') >= 0
                        || field.indexOf('\r') >= 0;
        if (!mustQuote) {
            return field;
        }
        return '"' + field.replace("\"", "\"\"") + '"';
    }
}
