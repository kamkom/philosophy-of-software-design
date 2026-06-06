package com.psd.ch08.before;

import com.psd.ch08.Clock;
import com.psd.ch08.FlushResult;
import com.psd.ch08.SimulatedSink;
import com.psd.ch08.Workload;
import com.psd.ch08.Workload.Arrival;

import java.util.List;

/**
 * Drives the {@code before} uploader through the standard workload.
 *
 * <p>The operator tuned the three knobs to look perfectly reasonable for the fast
 * downstream of phase 1. Then the downstream degrades and the static values go stale
 * (§8.2): the SLA is blown and the buffer overflows. Crucially, the overflow surfaces
 * as a {@link BufferFullException} that the two call sites below handle
 * <em>differently</em> — the clickstream drops loudly, the audit stream swallows
 * silently and loses data without anyone noticing. That divergence is exactly the
 * harm of "every caller of the class will have to deal with it" (§8 intro).
 */
public final class UploaderDemo {

    public static void main(String[] args) {
        Clock clock = new Clock();
        SimulatedSink sink = new SimulatedSink(clock, Workload.DEGRADE_AT_MILLIS,
                Workload.FAST_BASE_MILLIS, Workload.SLOW_BASE_MILLIS, Workload.PER_EVENT_MILLIS);

        // Knobs hand-tuned for phase 1's fast downstream. They look fine today.
        BatchingUploader uploader = new BatchingUploader(sink, clock,
                /* maxBatchSize     */ 5,
                /* flushIntervalMs  */ 50,
                /* maxBufferSize    */ 12);

        List<Arrival> arrivals = Workload.standard();

        int slaViolations = 0;
        int clicksDropped = 0; // call site A: drops and logs
        int auditLost = 0;     // call site B: swallows silently (a latent bug)

        System.out.println("=== before: hand-tuned knobs + BufferFullException ===");
        System.out.printf("knobs: maxBatch=5, flushInterval=50ms, maxBuffer=12  | SLA target=%dms%n",
                Workload.TARGET_LATENCY_MILLIS);

        int i = 0;
        while (i < arrivals.size() || !uploader.isEmpty()) {
            long nextArrival = i < arrivals.size() ? arrivals.get(i).timeMillis() : Long.MAX_VALUE;
            long nextFlush = uploader.nextFlushTimeMillis();

            if (nextFlush <= nextArrival) {
                clock.advanceTo(nextFlush);
                FlushResult result = uploader.flushDue();
                boolean violated = result.oldestEndToEndMillis() > Workload.TARGET_LATENCY_MILLIS;
                if (violated) {
                    slaViolations++;
                }
                System.out.printf("  flush: %2d events, oldest end-to-end = %3dms  %s%n",
                        result.batchSize(), result.oldestEndToEndMillis(),
                        violated ? "<-- SLA VIOLATED" : "");
            } else {
                clock.advanceTo(nextArrival);
                Arrival arrival = arrivals.get(i++);

                // Two call sites, two different reactions to the same exported exception.
                if (arrival.event().source().equals("clicks")) {
                    try {
                        uploader.offer(arrival.event());
                    } catch (BufferFullException e) {
                        clicksDropped++; // explicit, logged loss
                        System.out.println("    [clicks] " + e.getMessage() + " -> dropping event " + arrival.event().id());
                    }
                } else {
                    try {
                        uploader.offer(arrival.event());
                    } catch (BufferFullException e) {
                        auditLost++; // silent loss: the dangerous, inconsistent choice
                    }
                }
            }
        }

        System.out.println("--- summary ---");
        System.out.printf("events delivered downstream : %d%n", sink.totalEventsSent());
        System.out.printf("SLA violations              : %d%n", slaViolations);
        System.out.printf("clicks dropped (logged)     : %d%n", clicksDropped);
        System.out.printf("audit events lost (SILENT)  : %d%n", auditLost);
        System.out.println("knobs the operator had to choose: 3");
    }
}
