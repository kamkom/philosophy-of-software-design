package com.psd.ch08;

import java.util.ArrayList;
import java.util.List;

/**
 * A deterministic stand-in for the remote ingest API. A flush costs
 * {@code base + perEvent * batchSize} milliseconds, which it models by advancing
 * the shared {@link Clock}. The base cost jumps at {@code degradeAtMillis} to
 * simulate the downstream slowing down mid-run — the "operating conditions change"
 * scenario from §8.2 that makes any statically-configured cadence go stale.
 */
public final class SimulatedSink implements EventSink {

    private final Clock clock;
    private final long degradeAtMillis;
    private final long fastBaseMillis;
    private final long slowBaseMillis;
    private final long perEventMillis;
    private final List<List<Event>> sentBatches = new ArrayList<>();

    public SimulatedSink(Clock clock, long degradeAtMillis,
                         long fastBaseMillis, long slowBaseMillis, long perEventMillis) {
        this.clock = clock;
        this.degradeAtMillis = degradeAtMillis;
        this.fastBaseMillis = fastBaseMillis;
        this.slowBaseMillis = slowBaseMillis;
        this.perEventMillis = perEventMillis;
    }

    @Override
    public void send(List<Event> batch) {
        long base = clock.now() < degradeAtMillis ? fastBaseMillis : slowBaseMillis;
        clock.advance(base + perEventMillis * batch.size());
        sentBatches.add(List.copyOf(batch));
    }

    public int totalEventsSent() {
        return sentBatches.stream().mapToInt(List::size).sum();
    }
}
