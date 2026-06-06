package com.psd.ch10.after;

/**
 * The single exception type for everything that can legitimately fail one
 * command: an unknown command, a missing or malformed argument, a read of a
 * path that isn't there.
 *
 * <p>It carries a ready-to-print message, so the one top-level handler in
 * {@link Shell} needs no knowledge of specific errors — §10.7: "The error
 * message can be generated at the time the exception is thrown and included as a
 * variable in the exception record ... The top-level handler extracts the
 * message from the exception and incorporates it into the error response."
 *
 * <p>It is a single type used for every condition; were the conditions to need
 * distinguishing, they would become subclasses of this one base — "different
 * subclasses of the exception can be defined for different conditions" (§10.7) —
 * and the single handler would keep working unchanged.
 */
public class ShellError extends RuntimeException {
    public ShellError(String message) {
        super(message);
    }
}
