# Chapter 9 - Better Together or Better Apart?

## The principle

Chapter 9 asks the recurring question - should two pieces of functionality live
together or apart? - and gives several rules for answering it. This example takes
the **"apart"** rule that the chapter builds to its strongest example (the editor
undo `History` class): when a class mixes a **general-purpose mechanism** with the
**special-purpose code** of one particular use, pull them apart.

> "If a module contains a mechanism that can be used for several different
> purposes, then it should provide just that one general-purpose mechanism. It
> should not include code that specializes the mechanism for a particular use."
> (§9.4)

> **Red Flag: Special-General Mixture** - "a general-purpose mechanism also
> contains code specialized for a particular use of that mechanism ... creates
> information leakage between the mechanism and the particular use case." (§9.4)

The domain is a backend **provisioning workflow** with a *saga / compensating
transaction*: run a sequence of steps, and if one fails, undo the steps that
already succeeded, in reverse. The "undo completed steps in reverse order" loop
is a textbook general-purpose mechanism - and in the `before` it is trapped
inside provisioning-specific classes.

## The `before`

See [`before`](src/main/java/com/psd/ch09/before).

Two provisioner classes, each a single `provision(...)` method:

- [`TenantProvisioner`](src/main/java/com/psd/ch09/before/TenantProvisioner.java):
  create schema → create bucket → register DNS.
- [`WorkspaceProvisioner`](src/main/java/com/psd/ch09/before/WorkspaceProvisioner.java):
  create schema → issue API key → reserve seats.

Each method interleaves two unrelated things:

1. **Special-purpose** business logic - which services to call, in what order.
2. A hand-rolled **general-purpose** mechanism - `boolean ...Created` flags, a
   `try/catch`, and a reverse-order unwind in the `catch` block.

Because the mechanism lives *inside* the special-purpose class, it cannot be
reused - so `WorkspaceProvisioner` copies the entire flag/try/unwind skeleton,
**and** the shared first step (create/drop schema) is duplicated verbatim across
both classes:

> **Red Flag: Repetition** - "If the same piece of code (or code that is almost
> the same) appears over and over again, that's a red flag that you haven't found
> the right abstractions." (§9.3)

The information leakage is concrete: the `catch` block must list the undo calls
in the exact reverse of the creation order and keep them in sync *by hand*. A fix
to the unwind policy (e.g. making compensation best-effort) has to be repeated in
every workflow.

## The `after`

See [`after`](src/main/java/com/psd/ch09/after).

The key move (§9.7): *"separated the general-purpose part ... from the
special-purpose parts and put the general-purpose part in a class by itself. Once
that was done, the rest of the design fell out naturally."* The design splits into
the three categories §9.7 names:

| §9.7 category | Here | Knows about |
| --- | --- | --- |
| **General-purpose mechanism** | [`Saga<C>`](src/main/java/com/psd/ch09/after/Saga.java) | nothing about provisioning |
| **Specifics of each action** | [`Step<C>`](src/main/java/com/psd/ch09/after/Step.java) impls (`CreateSchemaStep`, …) | one operation each |
| **Policy** (which steps, what order) | [`TenantProvisioning`](src/main/java/com/psd/ch09/after/TenantProvisioning.java) / [`WorkspaceProvisioning`](src/main/java/com/psd/ch09/after/WorkspaceProvisioning.java) | the workflow shape |

`Saga<C>` runs the steps forward and, on a throw, compensates the completed ones
in reverse. It is **generic in its context and never reads it** - the mechanism
*"knows nothing about the information stored in the actions or how they implement
their undo and redo methods"* (§9.7). That ignorance is exactly what makes it
general-purpose; baking a concrete `ProvisioningContext` into it would re-wed the
mechanism to one use.

Each `Step` carries its own `execute`/`compensate` pair, recording the id it
created into the caller-owned `ProvisioningContext` so its own compensation can
tear that resource down. The shared `CreateSchemaStep` now exists **once** and
both workflows reuse the same instance. The unwind machinery (including the
best-effort "one failing compensation must not strand the rest" rule, which the
`before` never bothered with) lives in **one place**.

### Why keep the throw?

Ch08 deleted an exception because it punted complexity upward. Here the throw is
kept on purpose: it is the mechanism's *trigger to roll back*, not a punt. The
`Saga` absorbs it, compensates, and reports a `SagaFailedException` - the caller
never orchestrates partial rollback.

## The win (structural, not behavioural)

Both demos print **identical** output (verify with the commands below), including
the rollback that restores state after the `globex` DNS conflict. The improvement
is in the code, and it shows when you add the **third** workflow:

- `before`: a new class that copies the flag/try/reverse-unwind skeleton **again**
  (~10 lines of mechanism), plus another copy of any shared step.
- `after`: a new policy class - three `.add(...)` calls - reusing `Saga` and the
  existing steps. **Zero** new mechanism.

## Run it

```bash
mvn -q -pl ch09-better-together-or-better-apart compile
CP=$(mvn -q -pl ch09-better-together-or-better-apart dependency:build-classpath -Dmdep.outputFile=/dev/stdout | tail -1)
java -cp "ch09-better-together-or-better-apart/target/classes:$CP" com.psd.ch09.before.BeforeDemo
java -cp "ch09-better-together-or-better-apart/target/classes:$CP" com.psd.ch09.after.AfterDemo
```

## Takeaway

> "The decision to split or join modules should be based on complexity. Pick the
> structure that results in the best information hiding, the fewest dependencies,
> and the deepest interfaces." (§9.9)

When a general-purpose mechanism is buried inside special-purpose code, extracting
it into a class of its own - behind a small strategy interface the mechanism never
looks past - removes the duplication, kills the leakage, and makes the mechanism
reusable. The specifics and the policy then fall out into their own layers.

### Aside: grouping (not built)

The book's `History` adds *fences* so one user undo can unwind several actions as a
group. The analogue here is a **nested saga** (one step that is itself a sub-saga,
compensated as a unit). It is the same general/special separation applied once
more - the grouping *policy* would live with the high-level code, not in `Saga` -
so it is left as an exercise to keep the footprint focused.
