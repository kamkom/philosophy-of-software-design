package com.psd.ch06.after;

import java.time.Duration;
import java.time.Instant;

/**
 * A half-open span of time, {@code [start, end)} — start included, end excluded.
 *
 * <p>This is the general-purpose value type that replaces the leaky
 * {@code MeetingRequest}/{@code MeetingId} pair from the {@code before} package.
 * It is the time analog of the book's generic {@code Position}, deliberately
 * chosen "instead of {@code Cursor}, which reflects a specific user interface."
 * (§6.3) It carries nothing about meetings, lunches, or organizers — just a span
 * — so storage can talk about time without importing any caller vocabulary.
 *
 * <p>The half-open convention is the same one the book picks for {@code delete}:
 * it operates on "characters at positions greater than or equal to start but less
 * than end" (§6.3). Half-open ranges abut without overlapping, so 9:00–9:15 and
 * 9:15–9:30 are adjacent, not colliding — exactly what a calendar wants.
 */
public record TimeRange(Instant start, Instant end) {

    public TimeRange {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start must not be after end: " + start + " > " + end);
        }
    }

    public static TimeRange of(Instant start, Duration length) {
        return new TimeRange(start, start.plus(length));
    }

    public Duration length() {
        return Duration.between(start, end);
    }

    /** True when this range and {@code other} share at least one instant. */
    public boolean overlaps(TimeRange other) {
        // Half-open: touching endpoints (this.end == other.start) do NOT overlap.
        return start.isBefore(other.end) && other.start.isBefore(end);
    }
}
