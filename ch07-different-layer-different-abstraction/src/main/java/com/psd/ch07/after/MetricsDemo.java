package com.psd.ch07.after;

import java.time.Duration;
import java.time.Instant;

/**
 * Runnable illustration of the "after" design. Recording is now {@code record(when,
 * count)} — no index arithmetic in the caller — and the two queries are one-liners.
 * Same inputs as the "before" demo, so the printed numbers match: the refactor
 * changed the shape of the code, not its behavior.
 */
public final class MetricsDemo {

    public static void main(String[] args) {
        Instant start = Instant.parse("2026-06-06T09:00:00Z");
        BucketStore store = new BucketStore(start, Duration.ofMinutes(1), 10);  // 09:00–09:10

        Instant[] hits = {
                Instant.parse("2026-06-06T09:03:50Z"),
                Instant.parse("2026-06-06T09:04:10Z"),
                Instant.parse("2026-06-06T09:04:30Z"),
                Instant.parse("2026-06-06T09:05:00Z"),
                Instant.parse("2026-06-06T09:06:40Z"),
                Instant.parse("2026-06-06T09:07:05Z"),
        };
        for (Instant hit : hits) {
            store.record(hit, 1);   // no bucket index in sight
        }

        RateReport report = new RateReport(store);
        AlertChecker alert = new AlertChecker(store, 0.02);

        Instant from = Instant.parse("2026-06-06T09:03:40Z");
        Instant to = Instant.parse("2026-06-06T09:07:10Z");
        System.out.printf("avg rate %s..%s = %.4f req/s%n", from, to,
                report.averageRatePerSecond(from, to));
        System.out.println("alert firing at " + to + " = " + alert.isFiring(to));
    }

    private MetricsDemo() {
    }
}
