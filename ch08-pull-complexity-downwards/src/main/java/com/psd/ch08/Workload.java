package com.psd.ch08;

import java.util.ArrayList;
import java.util.List;

/**
 * The identical input fed to both the {@code before} and {@code after} demos so the
 * comparison is fair: the same events arrive at the same times, and the downstream
 * degrades at the same instant. Only the uploader differs.
 */
public final class Workload {

    /** When the simulated downstream slows down (ms into the run). */
    public static final long DEGRADE_AT_MILLIS = 160;

    /**
     * Downstream latency is dominated by a fixed per-flush cost (a round trip), which
     * is exactly why batching exists: it amortizes that cost over many events. The
     * per-event cost is negligible, so latency ~ base regardless of batch size. The
     * base jumps from {@code FAST} to {@code SLOW} at {@link #DEGRADE_AT_MILLIS}.
     */
    public static final long FAST_BASE_MILLIS = 30;
    public static final long SLOW_BASE_MILLIS = 120;
    public static final long PER_EVENT_MILLIS = 0;

    /**
     * The end-to-end latency budget. This is a <em>business</em> requirement: only
     * the user knows how fresh their analytics must be. §8.2's test — "will users be
     * able to determine a better value than we can determine here?" — answers YES for
     * this knob, so the {@code after} uploader keeps it and pulls everything else down.
     */
    public static final long TARGET_LATENCY_MILLIS = 300;

    /** An event arrives every {@code ARRIVAL_INTERVAL_MILLIS} until {@code END_MILLIS}. */
    public static final long ARRIVAL_INTERVAL_MILLIS = 10;
    public static final long END_MILLIS = 400;

    public record Arrival(long timeMillis, Event event) {
    }

    private Workload() {
    }

    public static List<Arrival> standard() {
        List<Arrival> arrivals = new ArrayList<>();
        int id = 0;
        for (long t = 0; t < END_MILLIS; t += ARRIVAL_INTERVAL_MILLIS) {
            String source = (id % 2 == 0) ? "clicks" : "audit";
            arrivals.add(new Arrival(t, new Event(id++, source)));
        }
        return arrivals;
    }
}
