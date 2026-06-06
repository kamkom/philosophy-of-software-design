package com.psd.ch06.before;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

/**
 * Runnable walkthrough of the special-purpose design. It works — but notice that
 * every line below maps to a method that exists <em>only</em> for that line. The
 * API and the use site grew up together, one shallow method at a time.
 */
public class SchedulingDemo {

    public static void main(String[] args) {
        MeetingScheduler scheduler = new MeetingScheduler();
        LocalDate day = LocalDate.of(2026, 6, 8);

        MeetingId standup = scheduler.book("Standup", "Ada",
                day.atTime(LocalTime.of(9, 0)).toInstant(ZoneOffset.UTC),
                day.atTime(LocalTime.of(9, 15)).toInstant(ZoneOffset.UTC));
        scheduler.book("Design review", "Grace",
                day.atTime(LocalTime.of(10, 0)).toInstant(ZoneOffset.UTC),
                day.atTime(LocalTime.of(11, 0)).toInstant(ZoneOffset.UTC));
        scheduler.reserveLunch(day);

        // "Extend the standup." By how, in which direction? The call site can't say.
        scheduler.extend(standup);

        System.out.println("=== before: agenda ===");
        scheduler.agenda().forEach(System.out::println);

        System.out.println();
        System.out.println("Want 'auto-book the first free 30 minutes'? There is no");
        System.out.println("method for it, and you can't compose one — you'd have to");
        System.out.println("add yet another method to Schedule. (§6.2)");
    }
}
