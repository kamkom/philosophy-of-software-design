package com.psd.ch07.after;

import java.time.Instant;

/**
 * The same dashboard query as before — but the bucket-walking and edge-proration
 * are gone, because the store now provides the rate abstraction directly. The
 * "higher level code that uses the class" is simplified exactly as §7.4 promises.
 *
 * <p>There is no longer any {@code MetricsService} between this caller and the
 * store. The pass-through facade was eliminated by "expos[ing] the lower level
 * class directly to the callers of the higher level class, removing all
 * responsibility for the feature from the higher level class" (§7.1, Figure
 * 7.1(b)). What made that safe to do is that the store finally offers the right
 * abstraction, so no middleman is needed to dress it up.
 */
public class RateReport {

    private final BucketStore store;

    public RateReport(BucketStore store) {
        this.store = store;
    }

    public double averageRatePerSecond(Instant from, Instant to) {
        return store.rateOver(from, to);
    }
}
