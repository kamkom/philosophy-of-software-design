package com.psd.ch04.before;

/**
 * A bank account with a balance in minor units (e.g. cents).
 *
 * <p>Amounts are plain {@code long} minor units to dodge floating-point rounding;
 * a single implied currency is assumed to keep the example focused on Chapter 4.
 *
 * <p>Note the public {@link #setBalanceMinor(long)} setter: it lets any
 * collaborator mutate the balance directly. That is exactly how the shallow
 * {@link Ledger} works below, and it is a form of information leakage — the
 * rule "money is only ever moved in balanced debit/credit pairs" lives nowhere
 * the type system can enforce it.
 */
public class Account {

    private final String id;
    private long balanceMinor;
    private boolean frozen;

    public Account(String id, long balanceMinor) {
        this.id = id;
        this.balanceMinor = balanceMinor;
    }

    public String id() {
        return id;
    }

    public long balanceMinor() {
        return balanceMinor;
    }

    public void setBalanceMinor(long balanceMinor) {
        this.balanceMinor = balanceMinor;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }
}
