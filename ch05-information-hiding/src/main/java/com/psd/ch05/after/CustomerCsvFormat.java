package com.psd.ch05.after;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The single module that owns the CSV dialect. Reading and writing were two
 * <em>times</em>, not two pieces of knowledge, so they belong together: "combine
 * the core mechanisms for reading and writing files into a single class. This
 * class will get used during both the reading and writing phases of the
 * application." (§5.3) The book reaches the same conclusion for the HTTP team:
 * "it would have been better to merge them into a single class... This provides
 * better information hiding, since it isolates all knowledge of the request
 * format in one class." (§5.5)
 *
 * <p>Every format decision now lives here exactly once:
 * <ul>
 *   <li><b>Column order</b> — the {@link Column} enum is the one source of truth;
 *       both {@link #write} and {@link #read} iterate it, so the two directions
 *       cannot drift out of order.</li>
 *   <li><b>Escaping</b> — {@link #escape} and {@link #tokenize} are defined side
 *       by side as a matched pair, so the inverse relationship is obvious and
 *       checkable rather than coincidental.</li>
 *   <li><b>Encodings & defaults</b> — delimiter, date pattern, and the header
 *       are private constants. The caller is never asked about them: "whenever
 *       possible, classes should 'do the right thing' without being explicitly
 *       asked." (§5.7) None of this appears in the interface, so it is "embedded
 *       in the module's implementation but does not appear in its interface."
 *       (§5.1)</li>
 * </ul>
 *
 * <p>The interface is just {@link #write} and {@link #read}: a deep module, since
 * "if a module hides a lot of information, that tends to increase the amount of
 * functionality provided by the module while also reducing its interface."
 * (§5.10)
 */
public class CustomerCsvFormat {

    // --- The dialect: declared once, hidden from every caller. ----------------

    /**
     * The columns, in their wire order. Each column knows how to pull its value
     * out of a {@link Customer} and how to push a parsed value back in. Because
     * order is defined here and nowhere else, "a design change related to that
     * information will affect only the one module." (§5.1)
     */
    private enum Column {
        ID("id") {
            String from(Customer c) {
                return c.id();
            }

            void into(Builder b, String v) {
                b.id = v;
            }
        },
        NAME("name") {
            String from(Customer c) {
                return c.name();
            }

            void into(Builder b, String v) {
                b.name = v;
            }
        },
        SIGNUP_DATE("signup_date") {
            String from(Customer c) {
                return c.signupDate().format(DATE_FORMAT);
            }

            void into(Builder b, String v) {
                b.signupDate = LocalDate.parse(v, DATE_FORMAT);
            }
        },
        BALANCE_MINOR("balance_minor") {
            String from(Customer c) {
                return Long.toString(c.balanceMinor());
            }

            void into(Builder b, String v) {
                b.balanceMinor = Long.parseLong(v);
            }
        },
        NOTES("notes") {
            String from(Customer c) {
                return c.notes();
            }

            void into(Builder b, String v) {
                b.notes = v;
            }
        };

        final String header;

        Column(String header) {
            this.header = header;
        }

        abstract String from(Customer customer);

        abstract void into(Builder builder, String value);
    }

    private static final char DELIMITER = ',';
    private static final String RECORD_SEPARATOR = "\r\n";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    // --- The interface: two methods, no dialect in sight. ---------------------

    public String write(List<Customer> customers) {
        StringBuilder out = new StringBuilder();
        appendRow(out, header());
        for (Customer c : customers) {
            List<String> fields = new ArrayList<>(Column.values().length);
            for (Column column : Column.values()) {
                fields.add(column.from(c));
            }
            appendRow(out, fields);
        }
        return out.toString();
    }

    public List<Customer> read(String csv) {
        List<List<String>> records = tokenize(csv);
        List<Customer> customers = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {   // row 0 is the header we wrote
            customers.add(toCustomer(records.get(i)));
        }
        return customers;
    }

    // --- Everything below is the hidden body. ---------------------------------

    private List<String> header() {
        List<String> names = new ArrayList<>(Column.values().length);
        for (Column column : Column.values()) {
            names.add(column.header);
        }
        return names;
    }

    private void appendRow(StringBuilder out, List<String> fields) {
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                out.append(DELIMITER);
            }
            out.append(escape(fields.get(i)));
        }
        out.append(RECORD_SEPARATOR);
    }

    private Customer toCustomer(List<String> fields) {
        Builder builder = new Builder();
        for (Column column : Column.values()) {
            column.into(builder, fields.get(column.ordinal()));
        }
        return builder.build();
    }

    /**
     * Escaping and {@link #tokenize} are inverses — kept together on purpose.
     */
    private String escape(String field) {
        boolean mustQuote =
                field.indexOf(DELIMITER) >= 0
                        || field.indexOf('"') >= 0
                        || field.indexOf('\n') >= 0
                        || field.indexOf('\r') >= 0;
        if (!mustQuote) {
            return field;
        }
        return '"' + field.replace("\"", "\"\"") + '"';
    }

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
                        field.append('"');
                        i += 2;
                    } else {
                        inQuotes = false;
                        i++;
                    }
                } else {
                    field.append(ch);
                    i++;
                }
            } else if (ch == '"') {
                inQuotes = true;
                i++;
            } else if (ch == DELIMITER) {
                current.add(field.toString());
                field.setLength(0);
                i++;
            } else if (ch == '\r') {
                i++;
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

    /**
     * Lets each {@link Column} fill in its own slot during a read.
     */
    private static final class Builder {
        String id;
        String name;
        LocalDate signupDate;
        long balanceMinor;
        String notes;

        Customer build() {
            return new Customer(id, name, signupDate, balanceMinor, notes);
        }
    }
}
