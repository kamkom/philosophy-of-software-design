package com.psd.ch06.before;

import java.time.Instant;

/**
 * The scheduler-facing description of "a meeting someone wants to book". This is
 * the {@code Selection} analog from §6.2: a rich, feature-shaped type that the UI
 * hands straight to the storage layer via {@link Schedule#bookMeeting}.
 *
 * <p>Handing this whole object to storage is what couples the two classes. The
 * storage layer does not care about a meeting's {@code title} or {@code organizer}
 * — it only needs a span of time — yet because the special-purpose method is
 * shaped around the UI feature ("book a meeting"), the caller's vocabulary is
 * dragged into storage anyway.
 */
public record MeetingRequest(String title, String organizer, Instant start, Instant end) {
}
