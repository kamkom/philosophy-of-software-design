package com.psd.ch04.before;

/**
 * Shallow module that mutates account balances directly.
 *
 * <p>The two methods are trivial, but the dangerous part is what they do
 * <em>not</em> encapsulate: that a transfer is a debit <em>and</em> a credit
 * that must both happen, in that order, as a unit. Splitting the pair across
 * two public calls pushes the responsibility for atomicity and ordering onto
 * the caller — if anything were to go wrong between {@link #debit} and
 * {@link #credit}, money would simply vanish, and nothing in this interface
 * warns you of that.
 */
public class Ledger {

    public void debit(Account account, long amountMinor) {
        account.setBalanceMinor(account.balanceMinor() - amountMinor);
    }

    public void credit(Account account, long amountMinor) {
        account.setBalanceMinor(account.balanceMinor() + amountMinor);
    }
}
