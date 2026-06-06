package com.psd.ch06.after;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Runnable walkthrough of the general-purpose design. Same calendar story as
 * {@code before.SchedulingDemo}, plus the feature the special-purpose API could
 * not support: an {@link AutoScheduler} that books the first free slot, reusing
 * the calendar with no new storage method.
 */
public class SchedulingDemo {

    public static void main(String[] args) {
        MeetingScheduler scheduler = new MeetingScheduler();
        LocalDate day = LocalDate.of(2026, 6, 8);

        TimeRange standup = scheduler.book("Standup",
                day.atTime(LocalTime.of(9, 0)).toInstant(ZoneOffset.UTC),
                day.atTime(LocalTime.of(9, 15)).toInstant(ZoneOffset.UTC));
        scheduler.book("Design review",
                day.atTime(LocalTime.of(10, 0)).toInstant(ZoneOffset.UTC),
                day.atTime(LocalTime.of(11, 0)).toInstant(ZoneOffset.UTC));
        scheduler.reserveLunch(day);

        // "Extend the standup by 15 minutes" — obvious from the call site now.
        scheduler.extend(standup, Duration.ofMinutes(15));

        System.out.println("=== after: agenda (scheduler features) ===");
        scheduler.agenda().forEach(System.out::println);

        // Second client, zero new storage methods: book the first free 30 minutes
        // from 9:00 onward. It lands at 9:30 (after the extended standup, before
        // the design review).
        AutoScheduler auto = new AutoScheduler(scheduler.calendar());
        Optional<TimeRange> slot = auto.bookFirstFreeSlot(
                day.atTime(LocalTime.of(9, 0)).toInstant(ZoneOffset.UTC),
                Duration.ofMinutes(30), "Focus time (auto)");

        System.out.println();
        System.out.println("=== after: AutoScheduler booked " + slot.orElseThrow() + " ===");
        System.out.println("(reused Calendar.findFreeGap + reserve — no new method needed)");
        System.out.println();
        System.out.println("=== after: final agenda ===");
        scheduler.agenda().forEach(System.out::println);
    }
}
