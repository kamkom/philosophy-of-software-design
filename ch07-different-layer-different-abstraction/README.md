# Chapter 7 - Different Layer, Different Abstraction

## The principle

> "In a well-designed system, each layer provides a different abstraction from the
> layers above and below it... If a system contains adjacent layers with similar
> abstractions, this is a red flag that suggests a problem with the class
> decomposition." (§7, opening)

This example dramatizes two faces of that one rule:

- **§7.1 Pass-through methods** - a class whose methods do nothing but forward to
  another class with the same signature. It "increase[s] the interface complexity
  of the class... but [doesn't] increase the total functionality of the system."
- **§7.4 Interface versus implementation** - a class whose public API mirrors its
  internal representation. "If the two have similar abstractions, then the class
  probably isn't very deep."

Both are the same mistake: an extra layer that repeats the abstraction of the
layer it sits on, instead of providing a new one.

## The domain

A request-rate metric. Counts are stored internally in a fixed array of
equal-width **time buckets** (one slot per minute) - a fine internal
representation, and the chapter's "fixed-size disk blocks" storage unit. Two
features read it: a dashboard `RateReport` ("avg req/sec between two instants")
and an `AlertChecker` ("is the 5-minute rate over threshold?").

## The `before`

See [`src/main/java/com/psd/ch07/before`](src/main/java/com/psd/ch07/before).

Two layers, both speaking **buckets**:

- `BucketStore` exposes `getBucket(i)`, `incrementBucket(i)`, `bucketCount()`,
  `start()`, `bucketWidth()` - its interface is a carbon copy of its `long[]`
  field (**§7.4**).
- `MetricsService` sits on top and forwards every one of those methods unchanged
  (**§7.1** pass-through). Only `incrementBucket` carries logic, and it is the
  trivial one-variable bounds check the book calls out.

Because neither layer offers a *time-range* abstraction, the callers are forced to
do the domain work themselves. `RateReport` and `AlertChecker` each contain an
**identical** `countBetween` block: walk the buckets, sum the interior ones, and
prorate the two partial buckets at the edges of the range. That is §7.4's
"nontrivial... duplicated and scattered" splitting code, made literal - two copies
of the same arithmetic.

## The `after`

See [`src/main/java/com/psd/ch07/after`](src/main/java/com/psd/ch07/after).

- `BucketStore` becomes **deep**: its interface is now `record(Instant, long)`,
  `countBetween(Instant, Instant)`, and `rateOver(Instant, Instant)` - a genuinely
  *different* abstraction from the `long[]` it still uses internally. The
  bucket-walking and edge-proration live here **once**. "The difference represents
  valuable functionality provided by the class." (§7.4)
- `MetricsService` is **deleted**. With the store finally offering the right
  abstraction, no facade is needed to dress it up; callers talk to the store
  directly - the §7.1 fix in Figure 7.1(b): "expose the lower level class directly
  to the callers... removing all responsibility for the feature from the higher
  level class."
- `RateReport` and `AlertChecker` collapse to one-liners over `rateOver`.

Both `MetricsDemo`s print the same numbers (`0.0238 req/s`, alert `false`) - the
refactor changed the code's shape, not its behavior.

## The edit that shows the payoff

Add a third reader - say a `CapacityPlanner` that also needs a rate over a window.

- **Before:** a *third* copy of the `countBetween` proration block. And the day
  you switch to variable-width buckets, you must find and fix all three in
  lock-step - the duplication is a latent correctness hazard, not just clutter.
- **After:** a `new RateReport(store)`-style one-liner; the proration is never
  rewritten, and the bucketing model can change in a single file.

## Aside - what a *non*-pass-through higher layer looks like

We deleted `MetricsService` because it added nothing. That is **not** an argument
that higher layers are bad - only that a layer must provide a *different*
abstraction to earn its place. If the system genuinely needed many named metrics,
the right higher layer would be a **registry**: `record("http.requests", instant,
1)`, `rateOver("http.requests", from, to)`, owning one `BucketStore` per name.

That layer keys by *metric name* - an abstraction the store (which knows only one
metric's time buckets) does not have - so it is a legitimate "different layer,
different abstraction," not a pass-through. We left it out of the built code
because the example never needs more than one metric, and inventing the need would
have blurred the before/after diff. The test is always §7.6: a layer is worth it
only if it "eliminate[s] some complexity that would be present in [its] absence."

## Takeaway

When two adjacent layers describe the world the same way - same method signatures
(pass-through), or an API that mirrors the storage (interface = implementation) -
the extra layer is paying rent without producing value. Make each layer say
something the others don't, or remove it.
