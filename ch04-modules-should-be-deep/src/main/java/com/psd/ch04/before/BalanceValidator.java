package com.psd.ch04.before;

/**
 * Shallow module. Each method is a one-line check the caller must remember to
 * invoke, in the right combination, before touching the {@link Ledger}.
 *
 * <p>"a shallow module is one whose interface is relatively complex in
 * comparison to the functionality that it provides." (§4.5) The validator knows
 * nothing about transfers; it just exposes its checks and trusts the caller to
 * orchestrate them — so the knowledge of <em>which</em> checks a transfer needs
 * leaks up into {@link TransferExample}.
 */
public class BalanceValidator {

    public void ensurePositive(long amountMinor) {
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("amount must be positive: " + amountMinor);
        }
    }

    public void ensureDifferent(Account from, Account to) {
        if (from.id().equals(to.id())) {
            throw new IllegalArgumentException("cannot transfer to the same account: " + from.id());
        }
    }

    public void ensureNotFrozen(Account account) {
        if (account.isFrozen()) {
            throw new IllegalStateException("account is frozen: " + account.id());
        }
    }

    public void ensureSufficient(Account account, long amountMinor) {
        if (account.balanceMinor() < amountMinor) {
            throw new IllegalStateException("insufficient funds in " + account.id());
        }
    }

    // Red Flag: Shallow Module (§4.5). The entire body is visible through the
    // signature, so this method "offers no abstraction, since all of its
    // functionality is visible through its interface" (§4.5). It is no simpler
    // to call balanceOf(account) than to write account.balanceMinor() directly —
    // a pure pass-through that adds an interface to learn and hides nothing.
    public long balanceOf(Account account) {
        return account.balanceMinor();
    }
}
