package com.psd.ch07.before;

import java.time.Duration;
import java.time.Instant;

/**
 * A monitoring check: "is the request rate over the last five minutes above the
 * alert threshold?" A completely different feature from {@link RateReport}, written
 * by (imagine) a different engineer on a different day.
 *
 * <p>And yet {@link #countBetween} below is character-for-character the same
 * bucket-walking, edge-prorating arithmetic as {@code RateReport.countBetween}.
 * Two copies now exist; a third caller would make three. That is precisely the
 * cost §7.4 names — the split/join code "duplicated and scattered across the
 * implementation" — and it is a direct consequence of the store exposing its
 * storage representation instead of a time-range abstraction.
 *
 * <p>If the bucketing model ever changes (say, variable-width buckets), <em>every</em>
 * copy has to be found and fixed in lock-step. The duplication is not just ugly;
 * it is a latent correctness hazard.
 */
public class AlertChecker {

    private final MetricsService metrics;
    private final double thresholdPerSecond;

    public AlertChecker(MetricsService metrics, double thresholdPerSecond) {
        this.metrics = metrics;
        this.thresholdPerSecond = thresholdPerSecond;
    }

    public boolean isFiring(Instant now) {
        Instant from = now.minus(Duration.ofMinutes(5));
        long count = countBetween(from, now);
        long seconds = Duration.between(from, now).toSeconds();
        double rate = seconds == 0 ? 0.0 : (double) count / seconds;
        return rate > thresholdPerSecond;
    }

    // --- Duplicated from RateReport, because the store forced us to. -----------

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
