# Chapter 5 - Information Hiding (and Leakage)

## The principle

A module should encapsulate the **design decisions** it embodies - "the knowledge
is embedded in the module's implementation but does not appear in its interface,
so it is not visible to other modules." (§5.1) The opposite is **information
leakage**: "a design decision is reflected in multiple modules... any change to
that design decision will require changes to all of the involved modules." (§5.2)

The most pernicious form is **back-door leakage** - two classes that share a
piece of knowledge without either of them exposing it in an interface, so the
coupling is invisible: "two classes both have knowledge of a particular file
format (perhaps one class reads files in that format and the other class writes
them)... if the format changes, both classes will need to be modified." (§5.2)
And the usual cause is **temporal decomposition** - carving a system up by the
order operations happen in (write now, read later) instead of by the knowledge
each task needs (§5.3).

The example is a **CSV report round-trip**. The serialized data is byte-for-byte
identical in both versions; only the **location of the format knowledge** moves.

## The `before`

See [`src/main/java/com/psd/ch05/before`](src/main/java/com/psd/ch05/before).

The code is split by **time**, not by knowledge:

```text
  write phase                        read phase
  ReportExporter.export(...)   --->  ReportImporter.parse(...)
```

The CSV **dialect** is one design decision, but it shows up at both times, so it
is duplicated across both classes:

| The shared format decision        | Lives in `ReportExporter`            | …and again in `ReportImporter`        |
|-----------------------------------|--------------------------------------|---------------------------------------|
| Column set **and order**          | `export` writes fields 0..4 by hand  | `toCustomer` reads fields 0..4 by hand|
| RFC-4180 escaping rule            | `escape(...)`                        | `tokenize(...)` (the hand-built inverse) |
| Delimiter, date pattern, header   | constructor knobs                    | constructor knobs                     |

The smells, in the book's vocabulary:

- **Temporal decomposition (§5.3).** "The structure of a system corresponds to
  the time order in which operations will occur." Reading and writing became two
  classes purely because they happen at two different times.
- **Back-door information leakage (§5.2).** Neither `export(List<Customer>)` nor
  `parse(String)` mentions the dialect in its signature, yet both depend on it
  totally. "Back-door leakage like this is more pernicious... because it isn't
  obvious."
- **Duplicated knowledge (§5.5).** The escaping rule is implemented twice - once
  forward, once inverse - so "parsing code was duplicated in both classes." The
  two copies can drift, and when they do the round-trip keeps *succeeding* while
  silently corrupting any field that needed quoting.
- **Leakage through the interface / missing defaults (§5.7).** The delimiter,
  date pattern, and header flag are pushed onto the **caller**, who must hand the
  *same* values to both sides. "If the caller does specify a value, it probably
  results in information leakage between the [library] and the caller." The
  format decision now lives in *three* places.

## The `after`

See [`src/main/java/com/psd/ch05/after`](src/main/java/com/psd/ch05/after).

One deep module, `CustomerCsvFormat`, owns the entire dialect. Reading and
writing are two methods on it, not two classes:

```java
CustomerCsvFormat format = new CustomerCsvFormat();
String csv          = format.write(customers);
List<Customer> back = format.read(csv);
```

Every format decision is stated exactly once:

- **Column order** is a private `Column` enum that both `write` and `read`
  iterate - the two directions *cannot* drift out of order, because there is only
  one order.
- **Escaping** (`escape`) and **un-escaping** (`tokenize`) sit side by side as a
  matched pair, so their inverse relationship is checkable instead of
  coincidental.
- **Delimiter, date pattern, header** are private constants. The caller is never
  asked - "classes should 'do the right thing' without being explicitly asked."
  (§5.7)

None of it appears in the interface, which is exactly §5.1 ("embedded in the
module's implementation but does not appear in its interface") and §5.5 ("isolate
all knowledge of the [format] in one class"). The result is a deeper module:
"if a module hides a lot of information, that tends to increase the amount of
functionality provided by the module while also reducing its interface." (§5.10)

## Why this is the chapter, not just "DRY"

The point isn't merely "don't repeat code." It's that **a single design decision
should affect a single module.** Watch what an ordinary format change costs.

Say product asks for a new `country` column.

```text
before - the knowledge is in three places, edit each in lockstep:
  1. ReportExporter.export   add row.add(escape(c.country()))   at the right index
  2. ReportExporter.HEADER   add the column name                at the right index
  3. ReportImporter.toCustomer  read fields.get(5)              at the SAME index
  (miss any one, or disagree on the index, and the round-trip corrupts silently)

after - the knowledge is in one place:
  1. add a COUNTRY entry to the Column enum.   write() and read() follow it.
```

The `before` change is the kind that compiles, passes a smoke test on the easy
rows, and breaks the one row with a quoted field in production. That is the
"isn't obvious" cost of back-door leakage made concrete.

## Run it

```bash
./mvnw -q -pl ch05-information-hiding compile
./mvnw -q -pl ch05-information-hiding exec:java -Dexec.mainClass=com.psd.ch05.before.ReportRoundTrip
./mvnw -q -pl ch05-information-hiding exec:java -Dexec.mainClass=com.psd.ch05.after.ReportRoundTrip
```

Both print identical CSV and both report `round-trip preserved everything? true`
- including Grace Hopper's `notes`, which contains a comma, an embedded quote,
and a newline (every case the escaping rule exists for). Same behavior; the only
difference is where the format knowledge lives.

## Takeaway

Don't decompose by *when* things happen - decompose by *what knowledge* each task
needs. "When designing modules, focus on the knowledge that's needed to perform
each task, not the order in which tasks occur." (§5.3) When the same knowledge
turns up in two modules, that's the red flag: pull it into one.
