package com.psd.ch08;

/**
 * What one flush did: how many events it sent, and the end-to-end latency of the
 * oldest event in the batch (arrival -> downstream acknowledgement). The demo uses
 * the latter to decide whether the {@link Workload#TARGET_LATENCY_MILLIS} SLA held.
 */
public record FlushResult(int batchSize, long oldestEndToEndMillis) {
}
