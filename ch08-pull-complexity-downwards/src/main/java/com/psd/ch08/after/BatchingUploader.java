package com.psd.ch08.after;

import com.psd.ch08.Clock;
import com.psd.ch08.Event;
import com.psd.ch08.EventSink;
import com.psd.ch08.FlushResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Batches events and flushes them downstream — the harder-to-implement way that
 * pulls the complexity down (§8). The implementation took on extra work; the
 * interface shrank to a single constructor argument and an {@link #offer} that
 * cannot fail.
 *
 * <p><b>The cadence is computed, not configured.</b> Rather than exporting
 * {@code maxBatchSize} and {@code flushIntervalMillis}, the uploader measures the
 * downstream's flush latency itself and derives the cadence from it — exactly the
 * move §8.2 prescribes: "the transport protocol could compute a reasonable value on
 * its own by measuring the response time for requests that succeed and then using a
 * multiple of this... computing the retry interval dynamically, so it will adjust
 * automatically if operating conditions change." Here we flush the oldest event just
 * early enough that, even after the measured downstream latency, it still meets the
 * SLA. Batch size is whatever accumulated in that window — an emergent value nobody
 * had to guess: "the right values could have been determined automatically" (§8.2).
 *
 * <p><b>Backpressure is absorbed, not thrown.</b> When the downstream slows, the
 * measured latency rises, the flush window {@code target - latency} shrinks toward
 * zero, and the uploader flushes ever more eagerly — coupling its throughput to
 * whatever the downstream can take. So {@link #offer} never refuses an event and
 * there is no exception for callers to handle. The class "solve[s] the problem
 * completely" (§8.2) and takes "a little bit of extra suffering... to reduce the
 * suffering of [its] users" (§8.4).
 */
public final class BatchingUploader {

    private record Buffered(Event event, long arrivalMillis) {
    }

    private final EventSink sink;
    private final Clock clock;

    /**
     * The one surviving knob. §8.2's test — "will users be able to determine a better
     * value than we can determine here?" — answers YES only for this one: the latency
     * budget is a business decision the user owns. Pulling it down too would be
     * overreach (§8.3): we would be guessing at a requirement we cannot know, the same
     * mistake as baking UI policy into a text class.
     */
    private final long targetLatencyMillis;

    /**
     * Safety multiple applied to the measured latency — the chapter's literal advice
     * is to use "a multiple of this" (§8.2). It leaves headroom so a slightly-slower-
     * than-average flush still lands inside the budget.
     */
    private static final double LATENCY_SAFETY_MULTIPLE = 5.0;

    private final Deque<Buffered> buffer = new ArrayDeque<>();

    /** Exponentially-weighted moving average of measured downstream flush latency. */
    private double measuredLatencyMillis = 0;

    public BatchingUploader(EventSink sink, Clock clock, long targetLatencyMillis) {
        this.sink = sink;
        this.clock = clock;
        this.targetLatencyMillis = targetLatencyMillis;
    }

    /** Always accepts the event. No knobs, no exceptions — the simple interface. */
    public void offer(Event event) {
        buffer.addLast(new Buffered(event, clock.now()));
    }

    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    /**
     * Flush the oldest event {@code target - measuredLatency} after it arrived, so it
     * still meets the SLA once the downstream has taken its measured toll.
     */
    public long nextFlushTimeMillis() {
        if (buffer.isEmpty()) {
            return Long.MAX_VALUE;
        }
        // Until we have measured the downstream once, we cannot size the window — so we
        // flush immediately to obtain a first latency sample (akin to a protocol's first
        // round-trip estimate). Thereafter the window is target - a multiple of latency.
        if (measuredLatencyMillis == 0) {
            return buffer.peekFirst().arrivalMillis();
        }
        long estimate = Math.round(measuredLatencyMillis * LATENCY_SAFETY_MULTIPLE);
        long lead = Math.max(0, targetLatencyMillis - estimate);
        return buffer.peekFirst().arrivalMillis() + lead;
    }

    /** Send everything buffered, and update the latency estimate from this flush. */
    public FlushResult flushDue() {
        long startMillis = clock.now();
        long oldestArrival = buffer.peekFirst().arrivalMillis();

        List<Event> batch = new ArrayList<>(buffer.size());
        while (!buffer.isEmpty()) {
            batch.add(buffer.pollFirst().event());
        }

        sink.send(batch); // advances the clock by the downstream's latency

        long sample = clock.now() - startMillis;
        measuredLatencyMillis = measuredLatencyMillis == 0
                ? sample
                : (measuredLatencyMillis * 3 + sample) / 4; // 25% weight on the new sample
        return new FlushResult(batch.size(), clock.now() - oldestArrival);
    }
}
