# Chapter 10 — Define Errors Out of Existence

## The principle

Exception handling is, in Ousterhout's words, "one of the worst sources of
complexity in software systems." The chapter's key lesson is to **reduce the
number of places where exceptions must be handled**, and it gives four techniques
for doing so. This example demonstrates the two most code-forward of them, under
that one banner:

> "in many cases the semantics of operations can be modified so that the normal
> behavior handles all situations and there is no exceptional condition to
> report (hence the title of this chapter)." (ch.10 intro)

1. **Define errors out of existence (§10.3 / §10.5).** Redefine an operation so
   the "error" becomes a normal outcome — Tcl's `unset` made idempotent, Java's
   `substring` made to clamp instead of throw `IndexOutOfBoundsException`.
2. **Exception aggregation (§10.7).** For the errors that genuinely remain, stop
   handling each one where it arises; let them propagate to a **single** handler
   (Figure 10.1 → 10.2).

The domain is a tiny **in-memory filesystem** driven by a **batch of shell-style
commands** (`mkdir`, `write`, `read <path> <begin> <end>`, `rm`). The command
loop is exactly the setting §10.7 calls for — "a system [that] processes a series
of requests."

## The `before`

See [`before`](src/main/java/com/psd/ch10/before).

[`FileSystem`](src/main/java/com/psd/ch10/before/FileSystem.java) throws at the
slightest provocation — `rm` of an absent path, `mkdir` of an existing directory,
any `read` of a non-file — and its `read` is shallow (whole-file only, no range).
That is the over-defensive style §10.2 names, and it makes the class shallow:

> "classes with lots of exceptions have complex interfaces, and they are
> shallower than classes with fewer exceptions." (§10.3)

The damage shows up in [`Shell`](src/main/java/com/psd/ch10/before/Shell.java),
where **two** problems pile up:

- **Errors that should not exist.** `handleRm` wraps `rm` in a `catch`-and-ignore
  to fake idempotency; `handleMkdir` does the same for "already exists"; and
  because `read` can't take a range, `handleRead` carries the index arithmetic
  §10.5 complains about — "a one-line method call now becomes 5–10 lines of code."
- **Real errors handled all over the place.** Every handler repeats its own
  `try/catch` that turns an exception into an `error:` line. This is Figure 10.1:
  "a separate exception handler for each call ... this results in duplicated code."

## The `after`

See [`after`](src/main/java/com/psd/ch10/after).

[`FileSystem`](src/main/java/com/psd/ch10/after/FileSystem.java) redefines the
operations so three of the four have no error case left to report:

| Operation | `before` | `after` | Citation |
| --- | --- | --- | --- |
| `mkdir` existing dir | throws | returns (goal already holds) | §10.3 |
| `rm` absent path | throws | returns (goal already holds) | §10.3 |
| `read` out-of-range | throws / caller clamps | clamps, returns the overlap | §10.5 |
| `read` missing file | throws | **still throws** | §10.10 |

This makes the class **deeper** — fewer exceptions in the interface, more work
behind it: §10.5, redefining `substring` this way "simplifies the API for the
method while increasing its functionality, so it makes the method deeper."

[`Shell`](src/main/java/com/psd/ch10/after/Shell.java) then aggregates what's
left. Genuine failures throw one [`ShellError`](src/main/java/com/psd/ch10/after/ShellError.java)
carrying a printable message; the per-command handlers vanish, and a **single**
`catch (ShellError)` at the top of the loop renders the error line and continues
to the next command (Figure 10.2). The argument extractors (`getArg`/`getInt`)
are this example's `getParameter`: they describe their own errors but know
nothing about how an error line is rendered.

### Two lines worth not crossing (§10.8, §10.10)

- **`rm /ghost` is a no-op, but `read /ghost` is an error.** Same missing path,
  opposite decision — because `rm`'s goal ("be gone") is already met, while
  `read` needs the file. Defining the *missing file* away on read would be
  "taking it too far": "when something is important, it must be exposed." (§10.10)
- **The handler catches `ShellError`, not `Exception`.** A real bug (an NPE, say)
  is *not* aggregated — it crashes the loop, "clearly distinguished from
  exceptions that are fatal to the entire system" (§10.7), per §10.8's "just
  crash." Aggregation must not become a place to bury bugs.

The empty string returned by a fully out-of-range `read` is itself a special case
defined out of existence (§10.9): an ordinary value the caller handles with no
extra code, the way "Python returns an empty result for out-of-range list slices."

## The win (structural, not behavioural)

Both demos run the identical script and print the **identical transcript** (verify
below). The improvement is in the code:

- **`read` handler:** ~10 lines (arg parsing + two `catch`es + manual clamp) → a
  single `fs.read(path, begin, end)`.
- **`rm` / `mkdir` handlers:** a `try/catch`-to-fake-idempotency → one line.
- **Add a fifth command** (say `mv`): `before` needs a new handler *and* its own
  `try/catch` error plumbing; `after` needs one `case` that throws `ShellError`
  for its problems and gets the error response for free.

## Run it

```bash
mvn -q -pl ch10-define-errors-out-of-existence compile
java -cp ch10-define-errors-out-of-existence/target/classes com.psd.ch10.before.BeforeDemo
java -cp ch10-define-errors-out-of-existence/target/classes com.psd.ch10.after.AfterDemo
```

Both print:

```
ok
ok
ok
hello
world
hell
ok
error: no such file: /docs/ghost.txt
error: /docs is a directory
error: unknown command: frobnicate
error: read: missing end argument
error: read: 'x' is not a number
ok
```

## Takeaway

> "Overall, the best way to reduce bugs is to make software simpler." (§10.5)

Most exceptions are a design choice, not a fact of nature. Before writing a
handler, ask whether the operation can be **redefined** so the error never
arises (idempotent `rm`/`mkdir`, clamping `read`); the API gets simpler *and*
more capable. For the errors that genuinely remain, **aggregate** them into one
handler instead of scattering `try/catch` at every call site — but keep exposing
the ones the caller truly needs, and let real bugs crash.
