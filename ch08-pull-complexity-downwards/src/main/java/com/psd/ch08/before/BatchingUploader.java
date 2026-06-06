package com.psd.ch08.before;

import com.psd.ch08.Clock;
import com.psd.ch08.Event;
import com.psd.ch08.EventSink;
import com.psd.ch08.FlushResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Batches events and flushes them downstream — the easy-to-implement way.
 *
 * <p>This class takes the path the chapter warns against: it "solve[s] the easy
 * problems and punt[s] the hard ones to someone else" (§8 intro). Two hard problems
 * are punted:
 *
 * <ol>
 *   <li><b>The batching policy is exported as configuration parameters</b>
 *       ({@code maxBatchSize}, {@code flushIntervalMillis}, {@code maxBufferSize}).
 *       This "move[s] complexity upwards instead of down" (§8.2): the good values
 *       depend on the downstream's current latency and throughput, which this class
 *       can measure but the user cannot. Every operator "in every installation will
 *       have to learn how to set them" (§8 intro), and once the downstream slows
 *       down the chosen values are wrong — "configuration parameters can easily
 *       become out of date" (§8.2).</li>
 *   <li><b>Backpressure is punted via an exception.</b> When the buffer fills,
 *       {@link #offer} throws {@link BufferFullException} instead of deciding what to
 *       do, so "every caller of the class will have to deal with it" (§8 intro).</li>
 * </ol>
 *
 * The implementation is simple; the interface is not. The chapter's whole point is
 * that this is the wrong trade: "it is more important for a module to have a simple
 * interface than a simple implementation" (§8 intro).
 */
public final class BatchingUploader {

    private record Buffered(Event event, long arrivalMillis) {
    }

    private final EventSink sink;
    private final Clock clock;
    private final int maxBatchSize;
    private final long flushIntervalMillis;
    private final int maxBufferSize;

    private final Deque<Buffered> buffer = new ArrayDeque<>();
    private long lastFlushStartMillis = 0;

    public BatchingUploader(EventSink sink, Clock clock,
                            int maxBatchSize, long flushIntervalMillis, int maxBufferSize) {
        this.sink = sink;
        this.clock = clock;
        this.maxBatchSize = maxBatchSize;
        this.flushIntervalMillis = flushIntervalMillis;
        this.maxBufferSize = maxBufferSize;
    }

    /**
     * Accept one event for upload, or refuse it. The caller must be ready for the
     * refusal — that readiness is the complexity this class pushed upward.
     */
    public void offer(Event event) {
        if (buffer.size() >= maxBufferSize) {
            throw new BufferFullException(buffer.size());
        }
        buffer.addLast(new Buffered(event, clock.now()));
    }

    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    /** When the next flush is due, given the configured cadence and batch cap. */
    public long nextFlushTimeMillis() {
        if (buffer.isEmpty()) {
            return Long.MAX_VALUE;
        }
        if (buffer.size() >= maxBatchSize) {
            return clock.now();
        }
        return lastFlushStartMillis + flushIntervalMillis;
    }

    /** Send up to {@code maxBatchSize} buffered events downstream. */
    public FlushResult flushDue() {
        long startMillis = clock.now();
        long oldestArrival = buffer.peekFirst().arrivalMillis();

        int batchSize = Math.min(maxBatchSize, buffer.size());
        List<Event> batch = new ArrayList<>(batchSize);
        for (int i = 0; i < batchSize; i++) {
            batch.add(buffer.pollFirst().event());
        }

        sink.send(batch); // advances the clock by the downstream's latency
        lastFlushStartMillis = startMillis;
        return new FlushResult(batchSize, clock.now() - oldestArrival);
    }
}
