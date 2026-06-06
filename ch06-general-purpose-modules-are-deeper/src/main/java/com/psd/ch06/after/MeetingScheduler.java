package com.psd.ch06.after;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * The same scheduler features as {@code before.MeetingScheduler} — book, lunch,
 * extend, cancel — but now <em>composed</em> from {@link Calendar}'s four
 * primitives instead of forwarded to bespoke storage methods.
 *
 * <p>The code here is "a bit longer than with the original approach ... However,
 * the new code is more obvious." (§6.3) Each feature is now visibly built out of
 * reserve / release / shift, so a reader sees exactly what happens to the time
 * ranges without descending into storage.
 *
 * <p>Notice too where the knowledge moved: the "lunch is 12:00–13:00" policy now
 * lives in {@link #reserveLunch}, in the scheduler, where it belongs — not buried
 * in storage. Storage no longer knows what "lunch" is.
 */
public class MeetingScheduler {

    private final Calendar calendar = new Calendar();

    public TimeRange book(String title, Instant start, Instant end) {
        TimeRange range = new TimeRange(start, end);
        calendar.reserve(range, title);
        return range; // the caller holds the range; no MeetingId handle needed
    }

    /** The lunch policy is now the scheduler's, expressed in scheduler terms. */
    public TimeRange reserveLunch(LocalDate day) {
        TimeRange lunch = new TimeRange(
                day.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC),
                day.atTime(LocalTime.of(13, 0)).toInstant(ZoneOffset.UTC));
        calendar.reserve(lunch, "Lunch");
        return lunch;
    }

    /**
     * "Extend by 15 minutes" — the {@code before} package's false abstraction
     * (§6.4) — is now plain at the call site. Anyone reading this sees that the
     * <em>end</em> moves later and the start stays put. No need to go read storage
     * to find out which range changed.
     */
    public TimeRange extend(TimeRange meeting, Duration by) {
        TimeRange longer = new TimeRange(meeting.start(), meeting.end().plus(by));
        calendar.release(meeting);
        calendar.reserve(longer, "(extended)");
        return longer;
    }

    /** "Move the meeting earlier/later" — free composition via {@link Calendar#shift}. */
    public TimeRange move(TimeRange meeting, Duration by) {
        TimeRange moved = calendar.shift(meeting, by);
        calendar.release(meeting);
        calendar.reserve(moved, "(moved)");
        return moved;
    }

    public void cancel(TimeRange meeting) {
        calendar.release(meeting);
    }

    public List<String> agenda() {
        return calendar.describeReservations();
    }

    /** Exposed so a sibling client (e.g. {@link AutoScheduler}) can share the calendar. */
    Calendar calendar() {
        return calendar;
    }
}
