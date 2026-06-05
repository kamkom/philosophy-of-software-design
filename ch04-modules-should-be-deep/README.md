# Chapter 4 -Modules Should Be Deep

## The principle

A module is **deep** when its interface is much simpler than its implementation -
a lot of functionality reached through a small surface. The book frames this as
cost versus benefit: _"The benefit provided by a module is its functionality. The
cost of a module (in terms of system complexity) is its interface."_ (§4.4) The
best modules maximize benefit and minimize cost. A **shallow** module is the
opposite -its interface is nearly as complicated as the code behind it, so it
does little to fight complexity (§4.5).

The example is an idempotent money transfer. The *functionality* is identical in
both versions; only the **shape of the interface** changes.

## The `before`

See [`src/main/java/com/psd/ch04/before`](src/main/java/com/psd/ch04/before).

There is no transfer *module* at all. There are four shallow collaborators -
`BalanceValidator`, `IdempotencyStore`, `Ledger`, `ReceiptBuilder` -and the
actual transfer algorithm lives in **caller code** (`TransferExample.transfer`),
which must assemble them in exactly the right order:

```text
1. idempotency.contains(key)?  -> return stored receipt   (must run FIRST)
2. validator.ensurePositive / ensureDifferent / ensureNotFrozen / ensureSufficient
3. ledger.debit(from)  then  ledger.credit(to)            (order matters, no rollback)
4. receipts.build(...)
5. idempotency.record(key, receipt)                       (forget this -> double charge)
```

The smells, in the book's vocabulary:

- **Classitis (§4.6).** Lots of tiny classes, each with its own interface:
  _"These interfaces accumulate to create tremendous complexity at the system
  level."_ The marquee example in §4.7 is opening a Java file by hand-stacking
  `FileInputStream` + `BufferedInputStream` + `ObjectInputStream`; here the caller
  hand-stacks four payment collaborators.
- **Interface ≈ implementation (§4.1, §4.5).** To *use* these classes you must
  understand the whole protocol, so _"it is no simpler to think about the
  interface than to think about the full implementation."_
- **Pass-through red flag (§4.5).** `BalanceValidator.balanceOf(account)` just
  returns `account.balanceMinor()` -the book's `addNullValueForAttribute` in a
  new costume. It _"offers no abstraction, since all of its functionality is
  visible through its interface."_
- **Information leakage.** The "debit-before-credit, check-then-record" rules
  live only in the caller's head; `Account` even exposes a public balance setter,
  so nothing enforces that money moves in balanced pairs.

## The `after`

See [`src/main/java/com/psd/ch04/after`](src/main/java/com/psd/ch04/after).

One deep module, `PaymentService`, swallows the entire protocol. The headline
call is a single line:

```java
Receipt receipt = payments.transfer("alice", "bob", 2_500, "txn-001");
```

Behind that interface, hidden from every caller, sit:

- **Idempotent replay** -same key returns the original `Receipt`, no double charge.
- **Validation** -domain failures become a small sealed family of typed errors
  (`PaymentException.InsufficientFunds` / `AccountFrozen` / `AccountNotFound`);
  programmer misuse stays an `IllegalArgumentException`. Exceptions are part of
  the formal interface (§4.2).
- **Atomic two-leg posting** -all checks run before any balance moves, so money
  is never debited without being credited. `Account` is package-private with no
  public setter, so this invariant is actually enforceable.

This is §4.4 made concrete: _"a lot of functionality hidden behind a simple
interface,"_ where _"only a small fraction of its internal complexity is visible
to its users."_ The few extra methods (`openAccount`, `freeze`, `balanceOf`) are
honest plumbing, kept deliberately few -_"more, or larger, interfaces are not
necessarily better!"_ (§4.4)

## Side by side

Same accounts, same result. Only the interface changes.

```java
// before -the algorithm lives in the caller
BalanceValidator validator = new BalanceValidator();
IdempotencyStore idempotency = new IdempotencyStore();
Ledger ledger = new Ledger();
ReceiptBuilder receipts = new ReceiptBuilder();

if (idempotency.contains(key)) return idempotency.get(key);
validator.ensurePositive(amount);
validator.ensureDifferent(from, to);
validator.ensureNotFrozen(from);
validator.ensureSufficient(from, amount);
ledger.debit(from, amount);
ledger.credit(to, amount);
Receipt r = receipts.build(from.id(), to.id(), amount);
idempotency.record(key, r);

// after -the algorithm lives in the module
Receipt r = payments.transfer(fromId, toId, amount, key);
```

Figure 4.1, in ASCII -area is functionality, the top edge is interface cost:

```text
   before (shallow)                         after (deep)

  Validator IdempStore Ledger Receipts      transfer(from,to,amt,key)
  ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐        ┌───────────────────────┐   <- small interface
  │  │ │  │ │  │ │  │ │  │ │  │ │  │        │                       │
  └──┘ └──┘ └──┘ └──┘ └──┘ └──┘ └──┘        │   idempotency         │
   wide interface, thin modules;            │   validation          │
   the real logic spills into the           │   atomic posting      │
   caller and isn't hidden at all           │                       │
                                            └───────────────────────┘   <- large hidden body
```

## Run it

```bash
./mvnw -q -pl ch04-modules-should-be-deep compile
./mvnw -q -pl ch04-modules-should-be-deep exec:java -Dexec.mainClass=com.psd.ch04.before.TransferExample
./mvnw -q -pl ch04-modules-should-be-deep exec:java -Dexec.mainClass=com.psd.ch04.after.TransferExample
```

Both print the same thing, including the idempotent retry leaving Alice at 7,500
(not 5,000).

## Takeaway

Don't count classes -measure interface against implementation: the best module
buries a hard job behind a call you can't get wrong (§4.8).
```

