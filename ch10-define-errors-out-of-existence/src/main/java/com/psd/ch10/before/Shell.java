package com.psd.ch10.before;

import java.util.List;

/**
 * Runs a batch of shell-style commands against the {@link FileSystem} — the
 * {@code before} version, where the filesystem's exceptions have metastasised
 * into the caller.
 *
 * <p>Two diseases live here, both from the chapter:
 *
 * <ol>
 *   <li><b>Errors that should not exist at all.</b> {@code rm} of an absent path
 *       and {@code mkdir} of an existing directory are perfectly normal requests,
 *       yet the {@link FileSystem} throws — so each handler bolts on a defensive
 *       {@code catch}-and-ignore to fake the sane behaviour. And because
 *       {@link FileSystem#read(String)} is shallow, {@link #handleRead} carries
 *       the index arithmetic §10.5 says it should not have to.</li>
 *   <li><b>Genuine errors handled in scattered places.</b> Every handler repeats
 *       its own {@code try/catch} that turns an exception into an error line —
 *       this is Figure 10.1: "a separate exception handler for each call ... this
 *       results in duplicated code."</li>
 * </ol>
 *
 * <p>{@link com.psd.ch10.after.Shell} cures both: the first by defining the
 * errors out of existence (§10.3), the second by exception aggregation (§10.7).
 */
public final class Shell {

    private final FileSystem fs = new FileSystem();

    public void run(List<String> script) {
        for (String line : script) {
            String[] p = line.trim().split("\\s+");
            switch (p[0]) {
                case "mkdir" -> handleMkdir(p);
                case "write" -> handleWrite(p);
                case "read"  -> handleRead(p);
                case "rm"    -> handleRm(p);
                default      -> println("error: unknown command: " + p[0]);
            }
        }
    }

    private void handleMkdir(String[] p) {
        if (p.length < 2) {
            println("error: mkdir: missing path argument");
            return;
        }
        try {
            fs.mkdir(p[1]);
            println("ok");
        } catch (PathExistsException e) {
            // Defensive idempotency, bolted on because mkdir throws. The directory
            // already exists, so the work is done — but the API makes us say so by
            // catching. §10.3: with the right definition "its work is already done,
            // so it can simply return. There is no longer an error case to report."
            println("ok");
        } catch (NoSuchPathException e) {
            println("error: " + e.getMessage());
        }
    }

    private void handleWrite(String[] p) {
        if (p.length < 2) {
            println("error: write: missing path argument");
            return;
        }
        if (p.length < 3) {
            println("error: write: missing data argument");
            return;
        }
        try {
            fs.write(p[1], p[2]);
            println("ok");
        } catch (NoSuchPathException e) {
            println("error: " + e.getMessage());
        } catch (IsADirectoryException e) {
            println("error: " + e.getMessage());
        }
    }

    private void handleRead(String[] p) {
        if (p.length < 2) {
            println("error: read: missing path argument");
            return;
        }
        if (p.length < 3) {
            println("error: read: missing begin argument");
            return;
        }
        if (p.length < 4) {
            println("error: read: missing end argument");
            return;
        }
        int begin;
        int end;
        try {
            begin = Integer.parseInt(p[2]);
        } catch (NumberFormatException e) {
            println("error: read: '" + p[2] + "' is not a number");
            return;
        }
        try {
            end = Integer.parseInt(p[3]);
        } catch (NumberFormatException e) {
            println("error: read: '" + p[3] + "' is not a number");
            return;
        }
        String content;
        try {
            content = fs.read(p[1]);
        } catch (NoSuchPathException e) {
            println("error: " + e.getMessage());
            return;
        } catch (IsADirectoryException e) {
            println("error: " + e.getMessage());
            return;
        }
        // The shallow read() pushed the range handling up here. §10.5: "this
        // requires me to check each of the indices and round them up to zero or
        // down to the end of the string; a one-line method call now becomes 5–10
        // lines of code." Every caller that wants a slice repeats this.
        int len = content.length();
        int from = Math.max(0, Math.min(len, begin));
        int to = Math.max(0, Math.min(len, end));
        if (to < from) {
            to = from;
        }
        println(content.substring(from, to));
    }

    private void handleRm(String[] p) {
        if (p.length < 2) {
            println("error: rm: missing path argument");
            return;
        }
        try {
            fs.rm(p[1]);
            println("ok");
        } catch (NoSuchPathException e) {
            // Deleting an absent file is not really an error — the goal "this path
            // is gone" is already met. But because rm() throws, every caller that
            // means "ensure absent" must catch-and-ignore, exactly as Tcl users
            // "end up enclosing calls to unset in catch statements." (§10.3)
            println("ok");
        }
    }

    private static void println(String s) {
        System.out.println(s);
    }
}
