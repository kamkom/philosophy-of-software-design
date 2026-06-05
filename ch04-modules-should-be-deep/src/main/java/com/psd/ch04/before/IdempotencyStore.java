package com.psd.ch04.before;

import java.util.HashMap;
import java.util.Map;

/**
 * Shallow module: a thin wrapper over a {@link Map}. Its three methods barely
 * hide anything beyond {@code map.containsKey} / {@code map.get} / {@code map.put}.
 *
 * <p>Worse, the <em>protocol</em> it participates in is not encoded anywhere:
 * the caller must remember to {@link #contains check} a key before doing the
 * work and to {@link #record record} the result afterwards. Forget either half
 * and you silently lose idempotency. That ordering knowledge is part of this
 * module's real interface, yet it lives only in the caller's head (and in
 * comments in {@link TransferExample}).
 */
public class IdempotencyStore {

    private final Map<String, Receipt> seen = new HashMap<>();

    public boolean contains(String idempotencyKey) {
        return seen.containsKey(idempotencyKey);
    }

    public Receipt get(String idempotencyKey) {
        return seen.get(idempotencyKey);
    }

    public void record(String idempotencyKey, Receipt receipt) {
        seen.put(idempotencyKey, receipt);
    }
}
