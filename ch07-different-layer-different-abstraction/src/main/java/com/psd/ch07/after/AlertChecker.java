package com.psd.ch07.after;

import java.time.Duration;
import java.time.Instant;

/**
 * The same monitoring check — and the duplicated {@code countBetween} block is
 * simply gone. The bucket arithmetic that {@link RateReport} and this class each
 * carried a private copy of now exists once, inside {@link BucketStore#rateOver}.
 * The split/join code is no longer "duplicated and scattered across the
 * implementation" (§7.4); it has a single home.
 *
 * <p>With the proration centralized, changing the bucketing model is a one-file
 * edit instead of a hunt-and-fix across every caller — the dependency the
 * pass-through interface created has been removed along with it.
 */
public class AlertChecker {

    private final BucketStore store;
    private final double thresholdPerSecond;

    public AlertChecker(BucketStore store, double thresholdPerSecond) {
        this.store = store;
        this.thresholdPerSecond = thresholdPerSecond;
    }

    public boolean isFiring(Instant now) {
        return store.rateOver(now.minus(Duration.ofMinutes(5)), now) > thresholdPerSecond;
    }
}
