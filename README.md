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
| 5 | Information Hiding | [`ch05-information-hiding`](ch05-information-hiding) | ⏳ |
| 6 | General-Purpose Modules Are Deeper | [`ch06-general-purpose-modules-are-deeper`](ch06-general-purpose-modules-are-deeper) | ⏳ |
| 7 | Different Layer, Different Abstraction | [`ch07-different-layer-different-abstraction`](ch07-different-layer-different-abstraction) | ⏳ |
| 8 | Pull Complexity Downwards | [`ch08-pull-complexity-downwards`](ch08-pull-complexity-downwards) | ⏳ |
| 9 | Better Together or Better Apart? | [`ch09-better-together-or-better-apart`](ch09-better-together-or-better-apart) | ⏳ |
| 10 | Define Errors Out of Existence | [`ch10-define-errors-out-of-existence`](ch10-define-errors-out-of-existence) | ⏳ |

## Building

Requires JDK 21+ (code targets Java 21). Maven is bundled via the wrapper:

```bash
./mvnw validate      # verify the build is wired correctly
./mvnw compile       # compile every chapter module
```
