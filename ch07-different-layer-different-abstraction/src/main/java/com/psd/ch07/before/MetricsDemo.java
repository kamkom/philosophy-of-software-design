package com.psd.ch07.before;

import java.time.Duration;
import java.time.Instant;

/**
 * Runnable illustration of the "before" design. Notice that even <em>recording</em>
 * a hit forces the caller to convert an {@link Instant} into a bucket index by
 * hand — a third place that knows the storage model leaks through the interface.
 */
public final class MetricsDemo {

    public static void main(String[] args) {
        Instant start = Instant.parse("2026-06-06T09:00:00Z");
        Duration bucketWidth = Duration.ofMinutes(1);
        BucketStore store = new BucketStore(start, bucketWidth, 10);  // 09:00–09:10
        MetricsService metrics = new MetricsService(store);

        // Record hits. The caller has to map each instant to a bucket index itself,
        // because the service/store only understand indices.
        Instant[] hits = {
                Instant.parse("2026-06-06T09:03:50Z"),
                Instant.parse("2026-06-06T09:04:10Z"),
                Instant.parse("2026-06-06T09:04:30Z"),
                Instant.parse("2026-06-06T09:05:00Z"),
                Instant.parse("2026-06-06T09:06:40Z"),
                Instant.parse("2026-06-06T09:07:05Z"),
        };
        for (Instant hit : hits) {
            long secondsIn = Duration.between(start, hit).toSeconds();
            int index = (int) (secondsIn / bucketWidth.toSeconds());
            metrics.incrementBucket(index);
        }

        RateReport report = new RateReport(metrics);
        AlertChecker alert = new AlertChecker(metrics, 0.02);

        Instant from = Instant.parse("2026-06-06T09:03:40Z");
        Instant to = Instant.parse("2026-06-06T09:07:10Z");
        System.out.printf("avg rate %s..%s = %.4f req/s%n", from, to,
                report.averageRatePerSecond(from, to));
        System.out.println("alert firing at " + to + " = "
                + alert.isFiring(to));
    }

    private MetricsDemo() {
    }
}
