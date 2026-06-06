package com.psd.ch10.before;

import java.util.List;

/**
 * Runs the shared command script through the {@code before} {@link Shell}.
 * Prints the same transcript as {@link com.psd.ch10.after.AfterDemo} — the
 * change in the {@code after} version is structural, not behavioural.
 */
public final class BeforeDemo {

    /** Same script both sides, so the transcripts can be diffed line-for-line. */
    public static final List<String> SCRIPT = List.of(
            "mkdir /docs",
            "write /docs/readme.txt hello-world",
            "mkdir /docs",                    // already exists -> idempotent "ok" (§10.3)
            "read /docs/readme.txt 0 5",      // -> hello
            "read /docs/readme.txt 6 999",    // end past EOF, clamped -> world (§10.5)
            "read /docs/readme.txt -5 4",     // begin below 0, clamped -> hell (§10.5)
            "rm /docs/ghost.txt",             // absent -> idempotent "ok" (§10.3)
            "read /docs/ghost.txt 0 5",       // absent on READ -> real error (§10.10)
            "read /docs 0 5",                 // a directory -> error
            "frobnicate /docs",               // unknown command -> error
            "read /docs/readme.txt 0",        // missing end arg -> error (§10.7)
            "read /docs/readme.txt x 5",      // bad begin value -> error (§10.7)
            "rm /docs/readme.txt");           // real delete -> "ok"

    public static void main(String[] args) {
        new Shell().run(SCRIPT);
    }
}
