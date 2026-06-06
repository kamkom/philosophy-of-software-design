package com.psd.ch06.before;

import java.time.Instant;
import java.util.List;

/**
 * The client. Because {@link Schedule} already has a bespoke method for every
 * feature, this class looks deceptively thin — it mostly forwards calls. But that
 * thinness is the symptom, not a virtue: the real logic (overlap rules, the lunch
 * policy, what "extend" means) has been pushed down into storage, where the UI
 * developer can't see it and can't reuse it.
 *
 * <p>The deeper problem surfaces the moment you want something new. There is no
 * "auto-book the first free slot" feature here, and there <em>cannot</em> be one
 * without adding yet another method to {@link Schedule} — none of
 * {@code bookMeeting}/{@code blockOutLunch}/{@code extendByFifteenMinutes} compose
 * into it. See {@code com.psd.ch06.after.AutoScheduler} for the client that the
 * general-purpose API supports for free.
 */
public class MeetingScheduler {

    private final Schedule schedule = new Schedule();

    public MeetingId book(String title, String organizer, Instant start, Instant end) {
        // The caller must build a storage-shaped request object just to hand its
        // own data across the boundary.
        return schedule.bookMeeting(new MeetingRequest(title, organizer, start, end));
    }

    public void reserveLunch(java.time.LocalDate day) {
        schedule.blockOutLunch(day);
    }

    public void extend(MeetingId id) {
        // What does this actually do to the meeting's time range? The name won't
        // tell you — you have to go read Schedule.extendByFifteenMinutes. (§6.4)
        schedule.extendByFifteenMinutes(id);
    }

    public void cancel(MeetingId id) {
        schedule.cancelMeeting(id);
    }

    public List<String> agenda() {
        return schedule.describeBookings();
    }
}
