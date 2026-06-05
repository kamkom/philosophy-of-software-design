package com.psd.ch04.before;

import java.util.UUID;

/**
 * Shallow module whose one method is a constructor call in disguise. It exists
 * mainly to grow the class count — a small symptom of "classitis" (§4.6),
 * where "classes are good, so more classes are better." It adds an interface to
 * learn while hiding essentially nothing.
 */
public class ReceiptBuilder {

    public Receipt build(String fromId, String toId, long amountMinor) {
        return new Receipt(UUID.randomUUID().toString(), fromId, toId, amountMinor, "COMPLETED");
    }
}
