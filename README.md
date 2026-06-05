# A Philosophy of Software Design — Worked Examples

Personal notes and runnable Java examples for each chapter of
**_A Philosophy of Software Design_** by John Ousterhout.

For each chapter I build a Maven module containing the **same code two ways** —
a `before` version that exhibits the problem the chapter describes, and an
`after` version that applies the chapter's principle — and write a short
article (the module's `README.md`) explaining the contrast in my own words.

## Layout

- One Maven module per chapter: `chNN-<slug>` (e.g. `ch04-modules-should-be-deep`).
- Inside each module:
  - `src/main/java/com/psd/chNN/before/` — the problematic version.
  - `src/main/java/com/psd/chNN/after/`  — the improved version.
  - `README.md` — the article (Principle → Before → After → Takeaway).
- New chapters are scaffolded with the `/new-chapter` skill (see `.claude/skills/new-chapter`).

## Chapters

| Ch. | Title | Module | Status |
|----:|-------|--------|:------:|
| 4 | Modules Should Be Deep | [`ch04-modules-should-be-deep`](ch04-modules-should-be-deep) | ⏳ |

## Building

Requires JDK 21+ (code targets Java 21). Maven is bundled via the wrapper:

```bash
./mvnw validate      # verify the build is wired correctly
./mvnw compile       # compile every chapter module
```
