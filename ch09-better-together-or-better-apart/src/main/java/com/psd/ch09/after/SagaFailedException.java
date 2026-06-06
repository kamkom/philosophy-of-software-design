package com.psd.ch09.after;

/**
 * Raised by {@link Saga#run} after a step failed and the completed steps were
 * compensated. The triggering failure is the {@linkplain #getCause() cause};
 * any exceptions thrown <em>during</em> compensation are attached as
 * {@linkplain #getSuppressed() suppressed} exceptions.
 */
public class SagaFailedException extends RuntimeException {
    public SagaFailedException(Throwable cause) {
        super("saga step failed and completed steps were compensated", cause);
    }
}
