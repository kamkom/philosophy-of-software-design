package com.psd.ch06.before;

/**
 * A scheduler concept — the handle the UI uses to refer to a booked meeting.
 *
 * <p>This is the equivalent of the book's {@code Cursor}/{@code Selection}: a type
 * that "reflects a specific user interface" (§6.3). The trouble starts when it
 * leaks <em>downward</em> into the storage layer. Because {@link Schedule}'s
 * methods accept and return {@code MeetingId}, the storage class is now forced to
 * understand a caller-side abstraction, which is exactly the coupling the chapter
 * warns about: "Abstractions related to the user interface ... were reflected in
 * the text class; this increased the cognitive load for developers working on the
 * text class." (§6.2)
 */
public record MeetingId(long value) {
}
