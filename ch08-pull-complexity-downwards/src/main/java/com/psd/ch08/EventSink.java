package com.psd.ch08;

import java.util.List;

/**
 * The downstream the uploader flushes batches to (think: a remote ingest API).
 * Its per-flush latency is real and varies at runtime — and it is observable only
 * <em>inside</em> the uploader, never by the uploader's callers. That asymmetry is
 * the crux of §8.2: the right batching cadence depends on a value "low-level
 * infrastructure code" is far better placed to learn than any user is to guess.
 */
public interface EventSink {

    void send(List<Event> batch);
}
