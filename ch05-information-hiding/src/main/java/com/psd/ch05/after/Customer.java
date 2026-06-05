package com.psd.ch05.after;

import java.time.LocalDate;

/**
 * Identical domain record to the {@code before} version — the fix is not about
 * the data, it is about where the <em>format knowledge</em> lives.
 *
 * @param balanceMinor balance in minor units (e.g. cents).
 */
public record Customer(
        String id,
        String name,
        LocalDate signupDate,
        long balanceMinor,
        String notes) {
}
