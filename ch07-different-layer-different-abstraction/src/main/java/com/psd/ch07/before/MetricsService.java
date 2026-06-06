package com.psd.ch07.before;

import java.time.Duration;
import java.time.Instant;

/**
 * The application-facing "service" layer that sits on top of {@link BucketStore}.
 * It looks like a respectable boundary — callers depend on {@code MetricsService},
 * not on the raw store — but it is almost entirely <strong>pass-through
 * methods</strong>.
 *
 * <p>"A pass-through method is one that does nothing except pass its arguments to
 * another method, usually with the same API as the pass-through method. This
 * typically indicates that there is not a clean division of responsibility between
 * the classes." (§7.1) Of the methods below, four are pure forwards and only
 * {@link #incrementBucket} carries any logic at all — a single trivial bounds
 * check — which is exactly the book's tally: "Of the four methods above, only the
 * last one has any functionality, and even there it is trivial: the method checks
 * the validity of one variable." (§7.1)
 *
 * <p>This layer makes the system <em>shallower</em>, not deeper: "Pass-through
 * methods make classes shallower: they increase the interface complexity of the
 * class, which adds complexity, but they don't increase the total functionality of
 * the system." (§7.1) It also couples the two classes for no benefit — "if the
 * signature changes for the [method] in [the store], then the [method] in [the
 * service] will have to change to match." (§7.1) Note how every signature here is a
 * carbon copy of {@link BucketStore}'s; that duplication is the red flag.
 */
public class MetricsService {

    private final BucketStore store;

    public MetricsService(BucketStore store) {
        this.store = store;
    }

    public int bucketCount() {
        return store.bucketCount();
    }

    public Instant start() {
        return store.start();
    }

    public Duration bucketWidth() {
        return store.bucketWidth();
    }

    public long getBucket(int index) {
        return store.getBucket(index);
    }

    /**
     * The one method with "any functionality, and even there it is trivial" (§7.1):
     * it validates a single argument and then forwards. The book's example was
     * {@code willInsertString}, which likewise did nothing but null-check one field
     * before delegating.
     */
    public void incrementBucket(int index) {
        if (index < 0 || index >= store.bucketCount()) {
            return;
        }
        store.incrementBucket(index);
    }
}
