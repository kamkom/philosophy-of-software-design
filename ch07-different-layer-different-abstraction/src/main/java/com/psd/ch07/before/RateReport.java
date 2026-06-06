package com.psd.ch07.before;

import java.time.Duration;
import java.time.Instant;

/**
 * A dashboard query: "what was the average request rate (req/sec) between
 * {@code from} and {@code to}?"
 *
 * <p>Because {@link MetricsService} only speaks buckets, this caller has to do the
 * domain work itself: walk the buckets, keep the ones fully inside the range,
 * and <em>prorate</em> the two partial buckets at the edges. That is the "nontrivial"
 * splitting code §7.4 describes — and the very same block reappears, copy-pasted,
 * in {@link AlertChecker}. "This code was nontrivial and it was duplicated and
 * scattered across the implementation." (§7.4)
 */
public class RateReport {

    private final MetricsService metrics;

    public RateReport(MetricsService metrics) {
        this.metrics = metrics;
    }

    public double averageRatePerSecond(Instant from, Instant to) {
        long count = countBetween(from, to);
        long seconds = Duration.between(from, to).toSeconds();
        return seconds == 0 ? 0.0 : (double) count / seconds;
    }

    /**
     * Estimated number of events in {@code [from, to)}. Interior buckets count in
     * full; an edge bucket only partially covered by the range is prorated by the
     * fraction of its width that overlaps — an estimate that assumes events are
     * spread uniformly within a bucket. <strong>This is the logic that should have
     * lived inside the store</strong>; instead every caller re-derives it.
     */
    private long countBetween(Instant from, Instant to) {
        long startSec = metrics.start().getEpochSecond();
        long widthSec = metrics.bucketWidth().toSeconds();
        long fromSec = from.getEpochSecond();
        long toSec = to.getEpochSecond();

        double total = 0.0;
        for (int i = 0; i < metrics.bucketCount(); i++) {
            long bucketStart = startSec + (long) i * widthSec;
            long bucketEnd = bucketStart + widthSec;
            long overlap = Math.min(bucketEnd, toSec) - Math.max(bucketStart, fromSec);
            if (overlap > 0) {
                total += metrics.getBucket(i) * ((double) overlap / widthSec);
            }
        }
        return Math.round(total);
    }
}
