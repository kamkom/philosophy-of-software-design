package com.psd.ch04.before;

/**
 * Proof that a transfer happened. Built by the shallow {@link ReceiptBuilder}
 * and stashed by the caller into the {@link IdempotencyStore}.
 */
public record Receipt(String transferId, String from, String to, long amountMinor, String status) {
}
