package com.psd.ch09.after;

import java.util.ArrayList;
import java.util.List;

/**
 * The general-purpose core, extracted into a class by itself. A {@code Saga}
 * runs a list of {@link Step}s in order; if any step throws, it walks the
 * already-completed steps backwards calling {@link Step#compensate}, then
 * reports the failure.
 *
 * <p>This is the move §9.7 calls the key one: "separated the general-purpose
 * part ... from the special-purpose parts and put the general-purpose part in
 * a class by itself. Once that was done, the rest of the design fell out
 * naturally." It is also §9.4's prescription literally applied — "pull the
 * special-purpose code upwards, into the higher layers, leaving the lower
 * layers general-purpose": the tenant/workspace specifics now live in the
 * {@link Step}s and assemblers above this class.
 *
 * <p>Crucially, this class "knows nothing about the information stored in the
 * actions or how they implement their undo and redo methods" (§9.7). It is
 * generic in {@code C} and never reads it, so the same mechanism serves any
 * workflow — that is what makes it general-purpose rather than a provisioning
 * detail.
 *
 * @param <C> the context type threaded through every step; opaque to the saga.
 */
public final class Saga<C> {
    private final List<Step<C>> steps = new ArrayList<>();

    public Saga<C> add(Step<C> step) {
        steps.add(step);
        return this;
    }

    public void run(C context) {
        List<Step<C>> completed = new ArrayList<>();
        for (Step<C> step : steps) {
            try {
                step.execute(context);
                completed.add(step); // only completed steps are eligible for compensation
            } catch (RuntimeException failure) {
                throw compensate(completed, context, failure);
            }
        }
    }

    /** Undo completed steps in reverse, best-effort: one failing compensation must not strand the rest. */
    private SagaFailedException compensate(List<Step<C>> completed, C context, RuntimeException failure) {
        SagaFailedException report = new SagaFailedException(failure);
        for (int i = completed.size() - 1; i >= 0; i--) {
            try {
                completed.get(i).compensate(context);
            } catch (RuntimeException compensationFailure) {
                report.addSuppressed(compensationFailure);
            }
        }
        return report;
    }
}
