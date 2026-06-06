package com.psd.ch06.after;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Storage for reserved time blocks — implemented with a <strong>somewhat
 * general-purpose</strong> API. Its methods are defined "only in terms of basic
 * [time] features, without reflecting the higher-level operations that will be
 * implemented with it." (§6.3)
 *
 * <p>Four primitives replace the open-ended pile of feature-shaped methods from
 * {@code before.Schedule}:
 * <ul>
 *   <li>{@link #reserve} / {@link #release} — the analogs of the book's
 *       {@code insert(position, text)} and {@code delete(start, end)} (§6.3). One
 *       way to claim time, one way to free it, regardless of <em>why</em>.</li>
 *   <li>{@link #shift} — the analog of {@code changePosition} (§6.3): pure range
 *       arithmetic the caller composes with, instead of a baked-in
 *       {@code extendByFifteenMinutes}.</li>
 *   <li>{@link #findFreeGap} — the analog of {@code findNext} (§6.3): the search
 *       primitive that turns a brand-new client (see {@link AutoScheduler}) into a
 *       few lines of composition rather than a new storage method.</li>
 * </ul>
 *
 * <p>This is "the simplest interface that will cover all my current needs": the
 * {@code before} class had three ways to delete-or-shrink-or-cancel; here, every
 * such operation is just {@link #release} (§6.5). And because no caller concept
 * (meeting, lunch, 15 minutes) appears anywhere below, "the [storage] class need
 * not be aware of specifics of the user interface ... New user interface features
 * can be added without creating new supporting functions." (§6.4)
 */
public class Calendar {

    /** Storage's atomic unit: a span plus an opaque label. */
    private record Reservation(TimeRange range, String label) {
    }

    private final List<Reservation> reservations = new ArrayList<>();

    /**
     * Claims {@code range}, tagging it with an opaque {@code label}. The label is
     * just text, like the arbitrary {@code String newText} the book's
     * {@code insert} accepts (§6.3) — storage neither parses nor interprets it.
     *
     * <p>Why a {@link TimeRange} and not a single {@link Instant}? A
     * {@code reserve(Instant)} that claimed one minute at a time would be even
     * simpler and even more general — but it would push every caller into loops and
     * be inefficient for large blocks. That is the §6.5 over-shoot the book warns
     * about: "it's better for the text class to have built-in support for
     * operations on ranges of characters" rather than single-character ones. So we
     * stop at <em>somewhat</em> general-purpose: ranges, not instants.
     *
     * @throws IllegalStateException if {@code range} overlaps an existing
     *     reservation. Storage enforces the no-double-booking invariant; callers
     *     that want to avoid the throw ask {@link #findFreeGap} first.
     */
    public void reserve(TimeRange range, String label) {
        for (Reservation r : reservations) {
            if (r.range().overlaps(range)) {
                throw new IllegalStateException(
                        "Time already reserved by '" + r.label() + "' over " + range);
            }
        }
        reservations.add(new Reservation(range, label));
    }

    /**
     * Frees time by removing every reservation that lies within {@code range}.
     * This is the {@code delete(start, end)} analog (§6.3): it is told a span, not
     * a meeting id, so it serves cancel, un-book, and "clear the afternoon" with
     * one method. Callers that hold the exact range of a single booking (the common
     * case) thereby release exactly that booking.
     */
    public void release(TimeRange range) {
        reservations.removeIf(r -> contains(range, r.range()));
    }

    /**
     * Returns a new range moved by {@code by} (positive = later, negative =
     * earlier), shifting both ends so the duration is preserved. This is pure
     * arithmetic — it touches no stored state — exactly like {@code changePosition}
     * "returns a new position that is a given number of characters away from a
     * given position." (§6.3)
     *
     * <p>It is what lets the caller express "extend", "move earlier", or "push to
     * tomorrow" itself, at the call site, where the effect is obvious — instead of
     * burying each as its own opaque storage method (§6.4).
     */
    public TimeRange shift(TimeRange range, Duration by) {
        return new TimeRange(range.start().plus(by), range.end().plus(by));
    }

    /**
     * Finds the earliest free span of at least {@code length} starting at or after
     * {@code from}. This is the {@code findNext} analog (§6.3): the one search
     * primitive that the scheduler needs anyway, and that a different client can
     * reuse "for free" — "the general-purpose text class would already have most of
     * the functionality needed for the new application." (§6.4)
     */
    public Optional<TimeRange> findFreeGap(Instant from, Duration length) {
        // Walk reservations in start order; the first gap big enough wins.
        List<Reservation> sorted = new ArrayList<>(reservations);
        sorted.sort((a, b) -> a.range().start().compareTo(b.range().start()));

        Instant candidate = from;
        for (Reservation r : sorted) {
            if (!r.range().end().isAfter(candidate)) {
                continue; // entirely before our search window
            }
            TimeRange gap = new TimeRange(candidate, r.range().start());
            if (!gap.start().isAfter(gap.end())
                    && gap.length().compareTo(length) >= 0) {
                return Optional.of(TimeRange.of(candidate, length));
            }
            if (r.range().end().isAfter(candidate)) {
                candidate = r.range().end();
            }
        }
        // No more reservations ahead — the open-ended gap after the last one fits.
        return Optional.of(TimeRange.of(candidate, length));
    }

    /** A read accessor for rendering. */
    public List<String> describeReservations() {
        List<Reservation> sorted = new ArrayList<>(reservations);
        sorted.sort(Comparator.comparing(a -> a.range().start()));
        List<String> out = new ArrayList<>();
        for (Reservation r : sorted) {
            out.add(r.label() + " [" + r.range().start() + " .. " + r.range().end() + ")");
        }
        return out;
    }

    private static boolean contains(TimeRange outer, TimeRange inner) {
        return !inner.start().isBefore(outer.start()) && !inner.end().isAfter(outer.end());
    }
}
