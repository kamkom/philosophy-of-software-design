package com.psd.ch04.after;

/**
 * Internal state owned entirely by {@link PaymentService}.
 *
 * <p>This class and its fields are package-private on purpose: nothing outside
 * this package can read or write a balance. The invariant "money only moves in
 * balanced pairs" is therefore enforceable, because the only code that can
 * mutate a balance is the deep module sitting right next to it. Contrast with
 * the {@code before} package, whose {@code Account} exposed a public setter.
 */
final class Account {

    final String id;
    long balanceMinor;
    boolean frozen;

    Account(String id, long balanceMinor) {
        this.id = id;
        this.balanceMinor = balanceMinor;
    }
}
