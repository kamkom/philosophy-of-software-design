package com.psd.ch06.after;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * A <strong>second, unrelated client</strong> of {@link Calendar}: "find the
 * earliest free slot of a given length and book it." The meeting scheduler never
 * asked for this, yet it needs <em>zero new methods</em> on storage — it is built
 * entirely from {@link Calendar#findFreeGap} and {@link Calendar#reserve}.
 *
 * <p>This is the chapter's reuse payoff. The book makes exactly this point with a
 * search-and-replace tool reusing the editor's text class: "the general-purpose
 * text class would already have most of the functionality needed for the new
 * application. All that is missing is a method to search ... Of course, an
 * interactive text editor is likely to have a mechanism for searching ... in which
 * case the text class would already include this method." (§6.3) Here too: the
 * scheduler already needs {@code findFreeGap} to avoid double-booking, so the
 * auto-scheduler gets its core primitive for free.
 *
 * <p>Try writing this against {@code before.Schedule} and you can't: none of its
 * feature-shaped methods compose into "first free slot", so you'd be forced to add
 * yet another storage method (§6.2).
 */
public class AutoScheduler {

    private final Calendar calendar;

    public AutoScheduler(Calendar calendar) {
        this.calendar = calendar;
    }

    /**
     * Books the earliest free span of {@code length} at or after {@code notBefore}.
     *
     * @return the slot that was booked, or empty if (somehow) none was found.
     */
    public Optional<TimeRange> bookFirstFreeSlot(Instant notBefore, Duration length, String label) {
        Optional<TimeRange> slot = calendar.findFreeGap(notBefore, length);
        slot.ifPresent(range -> calendar.reserve(range, label));
        return slot;
    }
}
