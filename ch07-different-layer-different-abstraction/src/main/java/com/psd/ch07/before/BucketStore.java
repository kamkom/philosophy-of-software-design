package com.psd.ch07.before;

import java.time.Duration;
import java.time.Instant;

/**
 * The storage layer for a request-rate metric. Counts are kept in a fixed array
 * of equal-width time buckets (e.g. one slot per minute), anchored at
 * {@code start}. This is the chapter's "fixed-size disk blocks" / "lines of text"
 * storage unit: a perfectly reasonable <em>internal representation</em>.
 *
 * <p>The mistake is that the <em>interface mirrors that representation</em>. Every
 * public method speaks in bucket indices, exactly the way the field does. "The
 * interface of a class should normally be different from its implementation: the
 * representations used internally should be different from the abstractions that
 * appear in the interface. If the two have similar abstractions, then the class
 * probably isn't very deep." (§7.4)
 *
 * <p>Nobody actually thinks in buckets. Callers think in <em>instants</em> and
 * <em>time ranges</em> ("how many requests between 09:03:40 and 09:07:10?"). With
 * a bucket-index API they are "forced to split and join lines to implement the
 * user-interface operations" (§7.4) — here, forced to convert instants to indices
 * and prorate the partial buckets at each edge of the range. As §7.4 warns, that
 * conversion "code [is] nontrivial and it [is] duplicated and scattered across the
 * implementation": see the identical arithmetic in {@link RateReport} and
 * {@link AlertChecker}.
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

    // --- A bucket-index interface: the same abstraction as the long[] field. ---

    public int bucketCount() {
        return buckets.length;
    }

    public Instant start() {
        return start;
    }

    public Duration bucketWidth() {
        return bucketWidth;
    }

    public long getBucket(int index) {
        return buckets[index];
    }

    public void incrementBucket(int index) {
        buckets[index]++;
    }
}
