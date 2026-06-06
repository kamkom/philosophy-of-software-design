package com.psd.ch06.before;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Storage for a day's reserved time blocks — implemented with a
 * <strong>special-purpose</strong> API. Each public method exists to serve exactly
 * one scheduler feature, so the class fills up with "a large number of shallow
 * methods, each of which [is] only suitable for one user interface operation."
 * (§6.2)
 *
 * <p>Watch what that costs:
 * <ul>
 *   <li><b>Information leaks downward.</b> {@link #bookMeeting} takes a
 *       {@link MeetingRequest} and {@link #cancelMeeting} takes a {@link MeetingId}
 *       — both caller concepts. Storage now has to know about meetings, titles,
 *       and organizers even though it really only manages spans of time. "This
 *       approach created information leakage between the user interface and the
 *       text class." (§6.2)</li>
 *   <li><b>Storage owns calendar policy.</b> {@link #blockOutLunch} hard-codes the
 *       12:00–13:00 rule <em>inside storage</em>. A policy that belongs to the
 *       scheduler has been buried one layer too deep, and the method is "invoked
 *       in a single place." (§6.2)</li>
 *   <li><b>Every new feature needs a new method here.</b> Want to extend a
 *       meeting? Add {@link #extendByFifteenMinutes}. Want to shorten one, or move
 *       it? Add more. "Each new user interface operation required a new method to
 *       be defined in the text class, so a developer working on the user interface
 *       was likely to end up working on the text class as well." (§6.2)</li>
 * </ul>
 *
 * <p>The result is a shallow module masquerading as an abstraction: the interface
 * is almost as large as the implementation, and the two classes can no longer be
 * developed independently (§6.2).
 */
public class Schedule {

    /** Storage's own atomic unit. Kept private; callers never see it. */
    private static final class Block {
        final MeetingId id;
        final String label;
        Instant start;
        Instant end;

        Block(MeetingId id, String label, Instant start, Instant end) {
            this.id = id;
            this.label = label;
            this.start = start;
            this.end = end;
        }
    }

    private final List<Block> blocks = new ArrayList<>();
    private long nextId = 1;

    /**
     * Feature: "book this meeting". The method is shaped around the UI's
     * {@link MeetingRequest}, so storage is forced to crack open a caller type and
     * even invents/returns a {@link MeetingId} — a handle that exists purely for
     * the UI's benefit.
     */
    public MeetingId bookMeeting(MeetingRequest request) {
        if (overlaps(request.start(), request.end())) {
            throw new IllegalStateException("Time already booked: " + request.title());
        }
        MeetingId id = new MeetingId(nextId++);
        blocks.add(new Block(id, request.title(), request.start(), request.end()));
        return id;
    }

    /**
     * Feature: "block out lunch for this day". This is the §6.2 red flag in its
     * purest form — a method "designed for one particular use". The 12:00–13:00
     * convention is a <em>scheduler</em> policy, but it lives down here in storage,
     * which now silently knows what "lunch" means.
     */
    public void blockOutLunch(LocalDate day) {
        Instant start = day.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC);
        Instant end = day.atTime(LocalTime.of(13, 0)).toInstant(ZoneOffset.UTC);
        if (overlaps(start, end)) {
            throw new IllegalStateException("Lunch slot already taken on " + day);
        }
        blocks.add(new Block(new MeetingId(nextId++), "Lunch", start, end));
    }

    /**
     * Feature: "extend the meeting by 15 minutes". This is the chapter's
     * <strong>false abstraction</strong> — the analog of {@code backspace} (§6.4).
     *
     * <p>It purports to hide a detail (which time range moves), but the scheduler
     * genuinely needs to know that detail: did the <em>end</em> move later, or did
     * the <em>start</em> move earlier? To answer, a UI developer must come down
     * here and read this code. "It purported to hide information ... but the user
     * interface module really needs to know this ... Hiding this information behind
     * an interface just creates obscurity." (§6.4)
     *
     * <p>It is also rigidly special-purpose: 15 minutes is baked in. "Extend by 30"
     * or "shorten by 10" each demand yet another shallow method.
     */
    public void extendByFifteenMinutes(MeetingId id) {
        Block block = find(id).orElseThrow(
                () -> new IllegalArgumentException("No such meeting: " + id));
        Instant newEnd = block.end.plus(Duration.ofMinutes(15));
        // Overlap check has to exclude the block being changed — fiddly logic the
        // caller can't see, so it can't reason about when an extend will fail.
        for (Block other : blocks) {
            if (other != block && other.start.isBefore(newEnd) && block.start.isBefore(other.end)) {
                throw new IllegalStateException("Cannot extend: collides with " + other.label);
            }
        }
        block.end = newEnd;
    }

    /** Feature: "cancel this meeting". One more caller-shaped method. */
    public void cancelMeeting(MeetingId id) {
        blocks.removeIf(b -> b.id.equals(id));
    }

    /** A read accessor the UI needs to render — yet another bespoke method. */
    public List<String> describeBookings() {
        List<String> out = new ArrayList<>();
        for (Block b : blocks) {
            out.add(b.label + " [" + b.start + " .. " + b.end + ")");
        }
        return out;
    }

    private boolean overlaps(Instant start, Instant end) {
        for (Block b : blocks) {
            if (b.start.isBefore(end) && start.isBefore(b.end)) {
                return true;
            }
        }
        return false;
    }

    private Optional<Block> find(MeetingId id) {
        for (Block b : blocks) {
            if (b.id.equals(id)) {
                return Optional.of(b);
            }
        }
        return Optional.empty();
    }
}
