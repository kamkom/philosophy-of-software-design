package com.psd.ch05.after;

import java.time.LocalDate;
import java.util.List;

/**
 * Caller code for the fixed design. Compare with the {@code before} runner: the
 * caller no longer knows — and cannot accidentally mismatch — the delimiter, the
 * date pattern, or the header. It asks one object to write and read. "The best
 * features are the ones you get without even knowing they exist." (§5.7)
 */
public class ReportRoundTrip {

    public static void main(String[] args) {
        List<Customer> customers = List.of(
                new Customer("c-001", "Ada Lovelace", LocalDate.of(2021, 3, 4), 12_500,
                        "VIP"),
                new Customer("c-002", "Grace Hopper", LocalDate.of(2019, 11, 9), 8_075,
                        "Said \"ship it\", then,\nfiled the bug report"));

        CustomerCsvFormat format = new CustomerCsvFormat();

        String csv = format.write(customers);
        List<Customer> restored = format.read(csv);

        System.out.println("=== after: CSV produced by the one format module ===");
        System.out.println(csv);
        System.out.println("=== after: round-tripped back through the same module ===");
        restored.forEach(System.out::println);
        System.out.println("round-trip preserved everything? "
                + customers.equals(restored));
    }
}
