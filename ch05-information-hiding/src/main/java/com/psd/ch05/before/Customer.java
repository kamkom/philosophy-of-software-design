package com.psd.ch05.before;

import java.time.LocalDate;

/**
 * The domain record. It is pure data and carries <em>no</em> knowledge of how a
 * customer is serialized — that is the whole point of the chapter. The CSV
 * "design decision" (column order, escaping, encodings) is the knowledge we are
 * watching for; here it lives nowhere near {@code Customer}, but — as the
 * exporter and importer show — it ends up smeared across two other classes.
 *
 * @param balanceMinor balance in minor units (e.g. cents), so money never rides
 *                     on a {@code double}.
 */
public record Customer(
        String id,
        String name,
        LocalDate signupDate,
        long balanceMinor,
        String notes) {
}
