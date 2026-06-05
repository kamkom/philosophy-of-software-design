package com.psd.ch04.after;

/**
 * The transfer failures a caller might reasonably want to handle, as a small
 * sealed family.
 *
 * <p>Exceptions are part of a module's formal interface: §4.2 lists "information
 * about exceptions thrown by the method" alongside parameter and return types.
 * A short, meaningful set of typed errors is good interface design, not
 * classitis — the caller learns three named outcomes instead of inspecting
 * balances and flags itself. Programmer-misuse cases (a non-positive amount, or
 * a transfer to the same account) are deliberately <em>not</em> here; those
 * throw {@link IllegalArgumentException}, because they signal a bug at the call
 * site rather than a domain condition worth catching.
 */
public sealed class PaymentException extends RuntimeException {

    private PaymentException(String message) {
        super(message);
    }

    /** The source account does not have enough balance to cover the transfer. */
    public static final class InsufficientFunds extends PaymentException {
        public InsufficientFunds(String accountId, long requestedMinor, long availableMinor) {
            super("insufficient funds in " + accountId
                    + ": requested " + requestedMinor + ", available " + availableMinor);
        }
    }

    /** The source account is frozen and cannot send money. */
    public static final class AccountFrozen extends PaymentException {
        public AccountFrozen(String accountId) {
            super("account is frozen: " + accountId);
        }
    }

    /** One of the referenced accounts does not exist. */
    public static final class AccountNotFound extends PaymentException {
        public AccountNotFound(String accountId) {
            super("no such account: " + accountId);
        }
    }
}
