package com.psd.ch04.before;

/**
 * The caller's-eye view of the shallow design. Notice where the actual transfer
 * <em>algorithm</em> lives: not in any module, but here, in {@link #transfer},
 * stitched together out of four collaborators.
 *
 * <p>This is the "classitis" failure from §4.6: "Small classes don't contribute
 * much functionality, so there have to be a lot of them, each with its own
 * interface. These interfaces accumulate to create tremendous complexity at the
 * system level." Every caller that wants to move money must re-learn the same
 * protocol — check idempotency first, run the right validations, debit before
 * credit, build a receipt, and remember to record the key — because none of it
 * is hidden behind a module.
 */
public class TransferExample {

    public static void main(String[] args) {
        Account alice = new Account("alice", 10_000);
        Account bob = new Account("bob", 0);

        // The caller must construct and wire up every collaborator itself —
        // compare with the Java I/O classitis in §4.7, where opening one file
        // requires assembling three stream objects by hand.
        BalanceValidator validator = new BalanceValidator();
        IdempotencyStore idempotency = new IdempotencyStore();
        Ledger ledger = new Ledger();
        ReceiptBuilder receipts = new ReceiptBuilder();

        Receipt receipt = transfer(alice, bob, 2_500, "txn-001",
                validator, idempotency, ledger, receipts);
        System.out.println("Transferred: " + receipt);

        // Retried with the same key. This is only idempotent because the caller
        // below remembered the check-then-record protocol — the modules can't
        // guarantee it on their own.
        Receipt replay = transfer(alice, bob, 2_500, "txn-001",
                validator, idempotency, ledger, receipts);
        System.out.println("Replay returned original receipt? " + replay.equals(receipt));
        System.out.println("Alice balance (minor units): " + alice.balanceMinor());
    }

    /**
     * The transfer algorithm — which, in a shallow design, is condemned to live
     * in caller code. Every step below is knowledge the caller is forced to
     * carry. "It is no simpler to think about the interface than to think about
     * the full implementation." (§4.5)
     */
    static Receipt transfer(Account from, Account to, long amountMinor, String idempotencyKey,
                            BalanceValidator validator, IdempotencyStore idempotency,
                            Ledger ledger, ReceiptBuilder receipts) {

        // 1. Idempotency — must run FIRST, or a retry double-charges.
        if (idempotency.contains(idempotencyKey)) {
            return idempotency.get(idempotencyKey);
        }

        // 2. Validation — the caller must know which checks exist and run them all.
        validator.ensurePositive(amountMinor);
        validator.ensureDifferent(from, to);
        validator.ensureNotFrozen(from);
        validator.ensureSufficient(from, amountMinor);

        // 3. Posting — the caller must know debit comes before credit, and that
        //    there is no rollback if something failed between the two legs.
        ledger.debit(from, amountMinor);
        ledger.credit(to, amountMinor);

        // 4. Receipt, then 5. record the idempotency key — forget either and the
        //    next retry silently double-charges.
        Receipt receipt = receipts.build(from.id(), to.id(), amountMinor);
        idempotency.record(idempotencyKey, receipt);
        return receipt;
    }
}
