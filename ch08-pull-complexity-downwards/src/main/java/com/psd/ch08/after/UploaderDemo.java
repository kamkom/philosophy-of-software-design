package com.psd.ch08.after;

import com.psd.ch08.Clock;
import com.psd.ch08.FlushResult;
import com.psd.ch08.SimulatedSink;
import com.psd.ch08.Workload;
import com.psd.ch08.Workload.Arrival;

import java.util.List;

/**
 * Drives the {@code after} uploader through the <em>same</em> workload as the
 * {@code before} demo, including the same mid-run downstream slowdown.
 *
 * <p>Nothing was tuned: the only input is the SLA the user already owns. The uploader
 * measures the downstream, shrinks its flush window as latency climbs, and keeps
 * meeting the budget — "adjust[ing] automatically [as] operating conditions change"
 * (§8.2). And because {@code offer} never throws, there is a single call site with no
 * exception handling and no events lost. Note the absence of a {@code BufferFullException}
 * import and of any source-specific call sites: that deleted complexity is the win.
 */
public final class UploaderDemo {

    public static void main(String[] args) {
        Clock clock = new Clock();
        SimulatedSink sink = new SimulatedSink(clock, Workload.DEGRADE_AT_MILLIS,
                Workload.FAST_BASE_MILLIS, Workload.SLOW_BASE_MILLIS, Workload.PER_EVENT_MILLIS);

        // The only input: the latency budget the user already knows.
        BatchingUploader uploader = new BatchingUploader(sink, clock, Workload.TARGET_LATENCY_MILLIS);

        List<Arrival> arrivals = Workload.standard();

        int slaViolations = 0;

        System.out.println("=== after: self-tuning uploader (one knob: the SLA) ===");
        System.out.printf("knobs: none to tune  | SLA target=%dms%n", Workload.TARGET_LATENCY_MILLIS);

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
                // One call site. No try/catch, no source-specific policy: offer cannot fail.
                uploader.offer(arrivals.get(i++).event());
            }
        }

        System.out.println("--- summary ---");
        System.out.printf("events delivered downstream : %d%n", sink.totalEventsSent());
        System.out.printf("SLA violations              : %d%n", slaViolations);
        System.out.println("events lost                 : 0");
        System.out.println("knobs the operator had to choose: 0");
    }
}
