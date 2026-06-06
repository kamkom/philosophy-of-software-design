package com.psd.ch09.after;

/**
 * One special-purpose operation in a {@link Saga}: it knows how to do exactly
 * one thing and how to undo that same thing.
 *
 * <p>This is the analogue of the book's {@code History.Action}. The steps are
 * "implemented outside the [Saga] class, in modules that understand particular
 * kinds of [operations]" (§9.7). Each implementation understands a single
 * operation and nothing about the mechanism that sequences it.
 *
 * @param <C> the workflow-owned context carrying data between steps. The
 *            mechanism never inspects {@code C}; only the steps do.
 */
public interface Step<C> {

    /** Perform the operation, recording any created resource ids into {@code context}. */
    void execute(C context);

    /**
     * Undo what {@link #execute} did, using the ids it recorded in {@code context}.
     * Called by {@link Saga} only for steps whose {@code execute} completed.
     */
    void compensate(C context);
}
