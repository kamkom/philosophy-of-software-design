# Chapter 8 - Pull Complexity Downwards

## The principle

When you hit a piece of unavoidable complexity while building a module, you can
either handle it *inside* the module or push it *up* to everyone who uses the
module. The chapter's rule: handle it yourself.

> "it is more important for a module to have a simple interface than a simple
> implementation" (§8 intro)

> "look for opportunities to take a little bit of extra suffering upon yourself in
> order to reduce the suffering of your users" (§8.4)

The chapter names two everyday ways developers do the opposite - pushing complexity
*upward* instead of pulling it down (§8 intro):

1. **Configuration parameters.** Rather than work out the right behaviour, you export
   knobs and make the user pick. "Every system administrator in every installation
   will have to learn how to set them," and the values "can easily become out of
   date" once conditions change (§8.2).
2. **Throwing an exception to punt a decision.** When you're "not certain how to deal
   with" a condition, "the easiest thing is to throw an exception and let the caller
   handle it" - so "every caller of the class will have to deal with it" (§8 intro).

This example commits both sins in `before` and pulls both down in `after`.

## The domain - a batch flusher

A **batch flusher** sits between a flood of small events (analytics clicks, audit
records, metrics) and a downstream service that ingests them. Sending one network
request per event is wasteful, so the flusher **accumulates events and ships them in
batches**, amortizing the fixed per-request cost over many events. That's why batching
exists.

The catch is timing. Flush too rarely and events go stale (you blow your freshness
SLA); flush too eagerly and you hammer the downstream with tiny batches. The *right*
cadence depends on **how fast the downstream is responding right now** - a value that
drifts at runtime and is visible only from *inside* the flusher (it's the one timing
the requests). The caller cannot know it.

- `EventSink` - the downstream ingest API. Its per-flush latency is real and changes
  mid-run.
- `Clock` - a manually-advanced clock so the simulation is deterministic and the
  printed numbers are reproducible (no real threads or wall-clock time).
- `SimulatedSink` - a fake downstream that models latency by advancing the clock, and
  **degrades partway through the run** (30 ms → 120 ms per flush) to play out §8.2's
  "operating conditions change."
- `Workload` - the identical input both demos run, so only the uploader differs.
- `BatchingUploader` - the class under study, in two versions.

## The `before`

See [`src/main/java/com/psd/ch08/before`](src/main/java/com/psd/ch08/before).

The implementation is easy because the hard decisions were shoved upward:

- **Three configuration parameters** - `maxBatchSize`, `flushIntervalMillis`,
  `maxBufferSize`. The operator tunes them to look perfectly reasonable for today's
  fast downstream (§8.2).
- **A `BufferFullException`** thrown from `offer` when the buffer fills, punting the
  backpressure decision to the caller (§8 intro).

Then the downstream slows down and the static knobs are wrong. Worse, the exception is
handled **inconsistently at the two call sites** - exactly the harm of "every caller
will have to deal with it":

- the *clicks* call site drops events and logs it;
- the *audit* call site swallows the exception silently and **loses data nobody
  notices**.

```
=== before: hand-tuned knobs + BufferFullException ===
knobs: maxBatch=5, flushInterval=50ms, maxBuffer=12  | SLA target=300ms
  flush:  5 events, oldest end-to-end =  70ms
  ...
    [clicks] upload buffer full at 12 events -> dropping event 36
    [clicks] upload buffer full at 12 events -> dropping event 38
  flush:  5 events, oldest end-to-end = 360ms  <-- SLA VIOLATED
  flush:  2 events, oldest end-to-end = 360ms  <-- SLA VIOLATED
--- summary ---
events delivered downstream : 35
SLA violations              : 2
clicks dropped (logged)     : 2
audit events lost (SILENT)  : 3
knobs the operator had to choose: 3
```

## The `after`

See [`src/main/java/com/psd/ch08/after`](src/main/java/com/psd/ch08/after).

The implementation took on extra work so the interface could shrink. The uploader
**measures the downstream's flush latency itself** and derives the cadence from it -
the move §8.2 prescribes for the network-retry example:

> "the transport protocol could compute a reasonable value on its own by measuring the
> response time for requests that succeed and then using a multiple of this... so it
> will adjust automatically if operating conditions change" (§8.2)

Concretely:

- It flushes the oldest event just early enough that, even after the *measured*
  downstream latency, it still meets the SLA. **Batch size is now emergent** - whatever
  accumulated in that window - so the `maxBatchSize` knob is gone: "the right values
  could have been determined automatically" (§8.2).
- When the downstream slows, the measured latency rises, the flush window shrinks
  toward zero, and the uploader flushes more eagerly - coupling throughput to whatever
  the downstream can take. So `offer` **never throws**: `BufferFullException`,
  `maxBufferSize`, and the two divergent catch blocks all disappear.

```
=== after: self-tuning uploader (one knob: the SLA) ===
knobs: none to tune  | SLA target=300ms
  flush:  1 events, oldest end-to-end =  30ms       <- warm-up probe: learn the latency
  flush: 17 events, oldest end-to-end = 270ms       <- absorbs the slowdown, still under budget
  flush: 16 events, oldest end-to-end = 157ms
  flush:  6 events, oldest end-to-end = 120ms
--- summary ---
events delivered downstream : 40
SLA violations              : 0
events lost                 : 0
knobs the operator had to choose: 0
```

The second batch is the moment the downstream degrades: the uploader rides through it
in a single batch (270 ms < 300 ms), then re-tunes and stays comfortably under budget -
"adjust[ing] automatically [as] operating conditions change" (§8.2). Nobody configured
anything; nothing was lost.

## Taking it too far (§8.3)

Pulling complexity down "is an idea that can easily be overdone" (§8.3). Two things the
`after` uploader **deliberately leaves upstairs**:

- **The latency SLA.** This is a business requirement only the user knows. §8.2's test -
  *"will users be able to determine a better value than we can determine here?"* -
  answers **yes**, so it stays a parameter. (It answers **no** for batch size and
  cadence, which is why those got pulled down.)
- **The choice of sink / serialization.** Baking those into the uploader would be the
  backspace-in-the-text-class mistake from §8.3: dragging unrelated knowledge inside a
  class doesn't simplify callers - it "just result[s] in information leakage."

The lesson isn't "absorb everything"; it's absorb the complexity that is *closely
related to the class's job, simplifies many callers, and shrinks the interface* (§8.3).

## Run it

```bash
mvn -q -pl ch08-pull-complexity-downwards compile
CP=ch08-pull-complexity-downwards/target/classes
java -cp "$CP" com.psd.ch08.before.UploaderDemo
java -cp "$CP" com.psd.ch08.after.UploaderDemo
```

Both feed on the identical `Workload`; only the uploader differs.

## Takeaway

The `before` is the easy implementation: throw an exception, export some knobs, ship
it. It pushed the hard parts onto every operator and every caller - and quietly lost
data when reality drifted from the configured guess. The `after` took "a little bit of
extra suffering" (§8.4) - measuring, deriving, absorbing backpressure - and in return
deleted a whole exception type, two of three knobs, and every call-site catch block.
A harder implementation bought a simpler, safer interface. That trade is the chapter.
