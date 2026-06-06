# Chapter 6 - General-Purpose Modules Are Deeper

## The principle

When you build a module, make it **somewhat general-purpose**: "the module's
functionality should reflect your current needs, but its interface should not.
Instead, the interface should be general enough to support multiple uses." (§6.1)
The surprising payoff is not (mainly) future reuse - it's that "the
general-purpose approach ... results in simpler and deeper interfaces than a
special-purpose approach." (§6.1)

A special-purpose interface, shaped one-method-per-caller-feature, produces "a
large number of shallow methods, each of which [is] only suitable for one user
interface operation" (§6.2) and leaks the caller's concepts down into storage,
tying the two classes together. A general-purpose interface - a few primitives
defined in terms of the module's *own* basic features - is deeper, composes
freely, and keeps the layers independent (§6.4).

The example is an **in-memory calendar / time-block allocator** consumed by a
meeting scheduler. The stored data and the behavior are identical in both
versions; only the **shape of the storage interface** changes.

> Mapping to the book's text-editor example: time instants ⇄ `Position`,
> `reserve`/`release(range)` ⇄ `insert`/`delete`, `shift` ⇄ `changePosition`,
> `findFreeGap` ⇄ `findNext`.

## The `before`

See [`src/main/java/com/psd/ch06/before`](src/main/java/com/psd/ch06/before).

`Schedule` has one method per scheduler feature. Every feature the UI grows
forces a new method down here:

| Scheduler feature        | Special-purpose storage method        | Smell |
|--------------------------|---------------------------------------|-------|
| Book a meeting           | `bookMeeting(MeetingRequest)`         | takes a caller type (`Selection` analog, §6.2) |
| Block lunch              | `blockOutLunch(LocalDate)`            | calendar policy buried in storage; called from one place (§6.2) |
| Extend a meeting         | `extendByFifteenMinutes(MeetingId)`   | **false abstraction** (§6.4) |
| Cancel a meeting         | `cancelMeeting(MeetingId)`            | yet another caller-shaped method |

The smells, in the book's vocabulary:

- **Shallow, single-use methods (§6.2).** Three of the four methods are invoked
  in exactly one place. "The text class ended up with a large number of shallow
  methods, each of which was only suitable for one user interface operation."
- **Information leakage between layers (§6.2).** `MeetingId` and `MeetingRequest`
  are UI concepts, yet storage must accept, store, and return them. "Abstractions
  related to the user interface ... were reflected in the text class; this
  increased the cognitive load for developers working on the text class."
- **False abstraction (§6.4).** `extendByFifteenMinutes` *purports* to hide which
  range changes - but the scheduler genuinely needs to know whether the end moved
  later or the start moved earlier, so a UI developer ends up reading the storage
  code anyway. "Hiding this information behind an interface just creates
  obscurity."
- **Every new operation = a new storage method (§6.2).** "Extend by 30" or
  "move to tomorrow" each demand another method. The UI and storage classes can
  no longer be developed independently.

## The `after`

See [`src/main/java/com/psd/ch06/after`](src/main/java/com/psd/ch06/after).

`Calendar` exposes four general-purpose primitives, defined "only in terms of
basic [time] features, without reflecting the higher-level operations that will
be implemented with it." (§6.3)

```java
void                reserve(TimeRange range, String label);   // ≈ insert
void                release(TimeRange range);                  // ≈ delete(start,end)
TimeRange           shift(TimeRange range, Duration by);       // ≈ changePosition
Optional<TimeRange> findFreeGap(Instant from, Duration length);// ≈ findNext
```

The scheduler now **composes** its features on top, and they read more
obviously at the call site (§6.3):

```java
// "Extend by 15 minutes" - no longer a mystery in storage:
TimeRange longer = new TimeRange(meeting.start(), meeting.end().plus(by));
calendar.release(meeting);
calendar.reserve(longer, "...");
```

What moved where:

- **`MeetingRequest`/`MeetingId` are gone.** The interface speaks `TimeRange` -
  the generic `Position`-analog "instead of `Cursor`, which reflects a specific
  user interface." (§6.3) Storage knows nothing about meetings.
- **The lunch policy moved up** into the scheduler, where it belongs.
- **The false abstraction is dissolved.** `extend`/`move` are spelled out in the
  scheduler from `shift`/`release`/`reserve`, so "the new code is more obvious."
  (§6.3)

### Stopping at *somewhat* (§6.5)

`reserve` takes a `TimeRange`, not a single `Instant`. A `reserve(Instant)`
minute-by-minute API would be even simpler and even more general - and worse:
callers would drown in loops and it would be inefficient for large blocks. That
is the §6.5 over-shoot: "it's better for the text class to have built-in support
for operations on ranges of characters" than single ones. General-purpose, but
not *maximally* - see the load-bearing comment on `Calendar.reserve`.

## Why this is the chapter, not just "fewer methods"

The real test is **a new, unplanned client**. `AutoScheduler` books the earliest
free slot of a given length:

```java
calendar.findFreeGap(notBefore, length).ifPresent(r -> calendar.reserve(r, label));
```

It needs **zero new storage methods** - it reuses `findFreeGap`, the very
primitive the scheduler already needs to avoid double-booking. This is the book's
search-and-replace story exactly: "the general-purpose text class would already
have most of the functionality needed for the new application. All that is
missing is a method to search ... in which case the text class would already
include this method." (§6.3)

Try writing `AutoScheduler` against `before.Schedule`: none of
`bookMeeting`/`blockOutLunch`/`extendByFifteenMinutes` compose into "first free
slot", so you would be forced to add yet another storage method (§6.2).

## Run it

```bash
./mvnw -q -pl ch06-general-purpose-modules-are-deeper compile
./mvnw -q -pl ch06-general-purpose-modules-are-deeper exec:java -Dexec.mainClass=com.psd.ch06.before.SchedulingDemo
./mvnw -q -pl ch06-general-purpose-modules-are-deeper exec:java -Dexec.mainClass=com.psd.ch06.after.SchedulingDemo
```

The `before` demo books and extends meetings, then prints why it *can't* offer
auto-booking. The `after` demo does the same scheduling **and** has
`AutoScheduler` drop a 30-minute focus block into the 9:30–10:00 gap - same
calendar, one extra client, no new storage method.

## Takeaway

Don't shape a module's interface around the features that happen to call it
today. Find "the simplest interface that will cover all my current needs" (§6.5)
- a few deep, general-purpose primitives - and let callers compose the
high-level operations themselves. "General-purpose interfaces ... tend to be
simpler, with fewer methods that are deeper. They also provide a cleaner
separation between classes ... Making your modules somewhat general-purpose is
one of the best ways to reduce overall system complexity." (§6.6)
```
