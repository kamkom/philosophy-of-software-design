package com.psd.ch07.after;

import java.time.Duration;
import java.time.Instant;

/**
 * The same metric, now a <strong>deep</strong> module. Internally it is still a
 * fixed array of time buckets — that representation was never the problem. What
 * changed is the interface: it speaks the caller's language ({@link Instant}s and
 * time ranges), not the storage's language (bucket indices).
 *
 * <p>"A character-oriented interface encapsulates the complexity of line splitting
 * and joining inside the text class, which makes the text class deeper and
 * simplifies higher level code that uses the class. With this approach, the text
 * API is quite different from the line-oriented storage mechanism; the difference
 * represents valuable functionality provided by the class." (§7.4) The
 * instant/range API here is the metric equivalent: the bucket-walking and
 * edge-proration that used to be copied into every caller now lives in exactly one
 * place — {@link #countBetween} — and the interface is genuinely a different
 * abstraction from the {@code long[]} behind it.
 *
 * <p>That difference is the justification for the class. "In order for an element
 * to provide a net gain against complexity, it must eliminate some complexity that
 * would be present in the absence of the design element." (§7.6) This one deletes
 * the duplicated proration math from two callers, so it pays its way.
 */
public class BucketStore {

    private final Instant start;
    private final Duration bucketWidth;
    private final long[] buckets;

    public BucketStore(Instant start, Duration bucketWidth, int bucketCount) {
        this.start = start;
        this.bucketWidth = bucketWidth;
        this.buckets = new long[bucketCount];
    }

    // --- The interface: instants and ranges, not bucket indices. --------------

    /** Record {@code count} events that happened at {@code when}. */
    public void record(Instant when, long count) {
        buckets[indexOf(when)] += count;
    }

    /**
     * Estimated number of events in {@code [from, to)}. Buckets fully inside the
     * range count in full; an edge bucket only partially overlapped is prorated by
     * the fraction of its width inside the range, assuming events are spread
     * uniformly within a bucket. The split/join complexity §7.4 wants encapsulated
     * is hidden here, once and for all.
     */
    public long countBetween(Instant from, Instant to) {
        long startSec = start.getEpochSecond();
        long widthSec = bucketWidth.toSeconds();
        long fromSec = from.getEpochSecond();
        long toSec = to.getEpochSecond();

        double total = 0.0;
        for (int i = 0; i < buckets.length; i++) {
            long bucketStart = startSec + (long) i * widthSec;
            long bucketEnd = bucketStart + widthSec;
            long overlap = Math.min(bucketEnd, toSec) - Math.max(bucketStart, fromSec);
            if (overlap > 0) {
                total += buckets[i] * ((double) overlap / widthSec);
            }
        }
        return Math.round(total);
    }

    /**
     * Average rate (events/second) over {@code [from, to)}. A one-line composition
     * on top of {@link #countBetween}: the deep class builds its own higher-level
     * operation instead of pushing that arithmetic onto callers.
     */
    public double rateOver(Instant from, Instant to) {
        long seconds = Duration.between(from, to).toSeconds();
        return seconds == 0 ? 0.0 : (double) countBetween(from, to) / seconds;
    }

    private int indexOf(Instant when) {
        long secondsIn = Duration.between(start, when).toSeconds();
        int index = (int) (secondsIn / bucketWidth.toSeconds());
        if (index < 0 || index >= buckets.length) {
            throw new IllegalArgumentException("instant outside store window: " + when);
        }
        return index;
    }
}
