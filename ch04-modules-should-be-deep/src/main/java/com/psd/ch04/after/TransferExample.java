package com.psd.ch04.after;

/**
 * The caller's-eye view of the deep design. Diff this against
 * {@code before/TransferExample}: there is no collaborator wiring, no protocol
 * to remember, no algorithm living in caller code. Moving money is a single
 * call, and the common case is as simple as it can be (§4.7).
 */
public class TransferExample {

    public static void main(String[] args) {
        PaymentService payments = new PaymentService();
        payments.openAccount("alice", 10_000);
        payments.openAccount("bob", 0);

        Receipt receipt = payments.transfer("alice", "bob", 2_500, "txn-001");
        System.out.println("Transferred: " + receipt);

        // Retried with the same key. Idempotency is guaranteed by the module,
        // not by the caller remembering a check-then-record dance.
        Receipt replay = payments.transfer("alice", "bob", 2_500, "txn-001");
        System.out.println("Replay returned original receipt? " + replay.equals(receipt));
        System.out.println("Alice balance (minor units): " + payments.balanceOf("alice")); // 7,500, not 5,000
    }
}
