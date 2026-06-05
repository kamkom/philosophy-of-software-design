package com.psd.ch04.after;

/**
 * Immutable proof of a completed transfer, returned by
 * {@link PaymentService#transfer}. It is also what idempotency memoizes: a
 * retry with the same key returns the very same {@code Receipt}.
 *
 * <p>There is only one {@link Status} — {@code COMPLETED} — because failures are
 * communicated as {@link PaymentException}s, not as receipt states. A caller
 * that holds a {@code Receipt} knows the money moved; there is no "did it
 * actually work?" follow-up question. That is part of what keeps the interface
 * simple.
 */
public record Receipt(String transferId, String from, String to, long amountMinor, Status status) {

    public enum Status {
        COMPLETED
    }
}
