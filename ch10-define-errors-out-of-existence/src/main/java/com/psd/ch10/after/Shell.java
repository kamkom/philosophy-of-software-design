package com.psd.ch10.after;

import java.util.List;

/**
 * Runs a batch of shell-style commands against the {@link FileSystem} — the
 * {@code after} version.
 *
 * <p>Compared with {@link com.psd.ch10.before.Shell}, the per-command handlers
 * have collapsed to one line each. Two changes did it:
 *
 * <ol>
 *   <li>The {@link FileSystem} no longer throws on the normal-but-unusual cases,
 *       so there is nothing to catch-and-ignore and no index arithmetic to carry
 *       (§10.3 / §10.5).</li>
 *   <li>The errors that <em>do</em> remain — unknown command, missing/bad
 *       argument, reading a missing path — are not handled where they arise.
 *       They propagate as a {@link ShellError} to the <b>single</b> handler in
 *       {@link #run}. This is Figure 10.2: "a single exception handler in the
 *       dispatcher catches all of the [errors] from all of the [command]
 *       methods."</li>
 * </ol>
 *
 * <p>{@code getArg}/{@code getInt} below are this example's {@code getParameter}
 * (§10.7): they "know how to describe extraction errors in a human-readable
 * form" but nothing about how an error line is rendered — that lives only in the
 * top-level handler.
 */
public final class Shell {

    private final FileSystem fs = new FileSystem();

    public void run(List<String> script) {
        for (String line : script) {
            // §10.7's request-loop pattern: one handler near the top of the loop
            // turns any command failure into one error line and continues with the
            // next command — "abort the current request, cleans up ..., and
            // continues with the next request."
            try {
                println(execute(line.trim().split("\\s+")));
            } catch (ShellError e) {
                // Caught here ONLY — deliberately not `catch (Exception)`. A genuine
                // bug (e.g. an NPE) is fatal to the loop and should crash (§10.8),
                // "clearly distinguished from exceptions that are fatal to the
                // entire system" (§10.7). Aggregation is not a place to bury bugs.
                println("error: " + e.getMessage());
            }
        }
    }

    private String execute(String[] p) {
        return switch (p[0]) {
            case "mkdir" -> {
                fs.mkdir(getArg(p, 1, "mkdir: missing path argument"));
                yield "ok";
            }
            case "write" -> {
                fs.write(getArg(p, 1, "write: missing path argument"),
                         getArg(p, 2, "write: missing data argument"));
                yield "ok";
            }
            case "read" -> fs.read(getArg(p, 1, "read: missing path argument"),
                                   getInt(p, 2, "read: missing begin argument"),
                                   getInt(p, 3, "read: missing end argument"));
            case "rm" -> {
                fs.rm(getArg(p, 1, "rm: missing path argument"));
                yield "ok";
            }
            default -> throw new ShellError("unknown command: " + p[0]);
        };
    }

    private static String getArg(String[] p, int index, String missingMessage) {
        if (index >= p.length) {
            throw new ShellError(missingMessage);
        }
        return p[index];
    }

    private static int getInt(String[] p, int index, String missingMessage) {
        String raw = getArg(p, index, missingMessage);
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new ShellError("read: '" + raw + "' is not a number");
        }
    }

    private static void println(String s) {
        System.out.println(s);
    }
}
