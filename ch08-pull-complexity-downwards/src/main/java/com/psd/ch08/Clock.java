package com.psd.ch08;

/**
 * A manually-advanced clock. The whole chapter turns on <em>measuring elapsed
 * time</em> ("measuring the response time for requests that succeed", §8.2), so a
 * real wall clock would make the demos non-reproducible. The simulated downstream
 * advances this clock to model how long a flush takes; the uploaders read it to
 * measure that latency.
 */
public final class Clock {

    private long nowMillis;

    public long now() {
        return nowMillis;
    }

    public void advance(long millis) {
        nowMillis += millis;
    }

    /** Move forward to {@code target} if it is in the future; never moves backwards. */
    public void advanceTo(long target) {
        if (target > nowMillis) {
            nowMillis = target;
        }
    }
}
