package com.psd.ch04.after;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A deep module: powerful functionality behind a simple interface.
 *
 * <p>"The best modules are deep: they have a lot of functionality hidden behind
 * a simple interface." (§4.4) The headline method, {@link #transfer}, is one
 * line to call, yet behind it sit idempotent replay, full validation, and
 * atomic two-leg posting — the entire six-step protocol the {@code before}
 * package forced onto its callers. None of that machinery is visible at the
 * interface, so "only a small fraction of its internal complexity is visible to
 * its users." (§4.4)
 *
 * <p>The supporting methods ({@link #openAccount}, {@link #freeze},
 * {@link #balanceOf}) are honest, unavoidable plumbing — you must be able to
 * create accounts and read balances. They are deliberately few: "interfaces are
 * good, but more, or larger, interfaces are not necessarily better!" (§4.4)
 */
public class PaymentService {

    private final Map<String, Account> accounts = new HashMap<>();
    private final Map<String, Receipt> processed = new HashMap<>();

    public void openAccount(String id, long initialBalanceMinor) {
        accounts.put(id, new Account(id, initialBalanceMinor));
    }

    public void freeze(String id) {
        require(id).frozen = true;
    }

    public long balanceOf(String id) {
        return require(id).balanceMinor;
    }

    /**
     * Move {@code amountMinor} from one account to another, exactly once per
     * {@code idempotencyKey}.
     *
     * <p>Everything below is hidden from the caller. Because every check runs
     * before any balance changes, the two-leg posting is effectively atomic:
     * there is no point at which money has left one account without arriving in
     * the other. A retry with a key we have already seen returns the original
     * receipt without touching balances again.
     *
     * @throws PaymentException.InsufficientFunds if the source cannot cover the amount
     * @throws PaymentException.AccountFrozen      if the source account is frozen
     * @throws PaymentException.AccountNotFound    if either account is unknown
     * @throws IllegalArgumentException            if the amount is non-positive or both ids are equal
     */
    public Receipt transfer(String fromId, String toId, long amountMinor, String idempotencyKey) {
        // 1. Idempotent replay — invisible to the caller, impossible to forget.
        Receipt prior = processed.get(idempotencyKey);
        if (prior != null) {
            return prior;
        }

        // 2. Validation — programmer misuse vs. domain failure, kept distinct.
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("amount must be positive: " + amountMinor);
        }
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("cannot transfer to the same account: " + fromId);
        }
        Account from = require(fromId);
        Account to = require(toId);
        if (from.frozen) {
            throw new PaymentException.AccountFrozen(fromId);
        }
        if (from.balanceMinor < amountMinor) {
            throw new PaymentException.InsufficientFunds(fromId, amountMinor, from.balanceMinor);
        }

        // 3. Atomic two-leg posting — both balances move together, after every
        //    failure path above has already been ruled out.
        from.balanceMinor -= amountMinor;
        to.balanceMinor += amountMinor;

        // 4. Receipt and 5. idempotency bookkeeping — handled here, once.
        Receipt receipt = new Receipt(
                UUID.randomUUID().toString(), fromId, toId, amountMinor, Receipt.Status.COMPLETED);
        processed.put(idempotencyKey, receipt);
        return receipt;
    }

    private Account require(String id) {
        Account account = accounts.get(id);
        if (account == null) {
            throw new PaymentException.AccountNotFound(id);
        }
        return account;
    }
}
