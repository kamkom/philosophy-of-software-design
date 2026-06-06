package com.psd.ch10.after;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The same in-memory filesystem as {@link com.psd.ch10.before.FileSystem}, with
 * the unnecessary errors defined out of existence (§10.3).
 *
 * <p>Three of the four operations no longer have an error case to report:
 * <ul>
 *   <li>{@link #mkdir} of an existing directory simply returns — the goal state
 *       already holds;</li>
 *   <li>{@link #rm} of an absent path simply returns — "ensure this path is
 *       gone" is already satisfied (the {@code unset} redefinition, §10.3);</li>
 *   <li>{@link #read} takes a range and clamps it, so no index is ever "out of
 *       bounds" — the {@code substring} redefinition, §10.5.</li>
 * </ul>
 *
 * <p>The result is a deeper class: fewer exceptions in the interface, more work
 * done behind it. §10.6 frames this kind of move as pulling complexity downward;
 * §10.5 notes it "simplifies the API for the method while increasing its
 * functionality, so it makes the method deeper."
 *
 * <p>Note what is <em>not</em> defined away: {@link #read} of a missing path
 * still throws. That is the §10.10 line — "when something is important, it must
 * be exposed." A clamped index is noise; a missing file the caller asked to read
 * is information it needs.
 *
 * <p>The tree model and lookup helpers below are identical to the {@code before}
 * version; only the four public operations differ.
 */
public final class FileSystem {

    private sealed interface Node permits Dir, FileNode {}
    private static final class Dir implements Node {
        final Map<String, Node> children = new TreeMap<>();
    }
    private record FileNode(String content) implements Node {}

    private final Dir root = new Dir();

    /**
     * Ensures a directory exists at {@code path}.
     *
     * <p>§10.3: "rather than deleting a variable, unset should ensure that a
     * variable no longer exists." Same redefinition here — mkdir ensures the
     * directory exists. If it already does, "its work is already done, so it can
     * simply return."
     */
    public void mkdir(String path) {
        Node existing = lookup(path);
        if (existing instanceof Dir) {
            return; // already a directory: nothing to do, no error to report
        }
        if (existing != null) {
            throw new ShellError(path + " exists and is not a directory");
        }
        Dir parent = parentDir(path);
        if (parent == null) {
            throw new ShellError("no such directory: " + parentPath(path));
        }
        parent.children.put(baseName(path), new Dir());
    }

    /** Creates or overwrites a file. */
    public void write(String path, String data) {
        Dir parent = parentDir(path);
        if (parent == null) {
            throw new ShellError("no such directory: " + parentPath(path));
        }
        if (lookup(path) instanceof Dir) {
            throw new ShellError(path + " is a directory");
        }
        parent.children.put(baseName(path), new FileNode(data));
    }

    /**
     * Returns the characters of the file with index {@code >= begin} and
     * {@code < end}, for <em>any</em> indices.
     *
     * <p>This is the §10.5 API verbatim: "returns the characters of the string
     * (if any) ... This is a simple and natural API, and it defines the
     * IndexOutOfBoundsException exception out of existence. The method's behavior
     * is now well-defined even if one or both of the indexes are negative, or if
     * beginIndex is greater than endIndex."
     *
     * <p>The empty result for a fully out-of-range request is itself a special
     * case defined out of existence (§10.9): the empty string is an ordinary
     * return value the caller handles with no extra code — like Python, which
     * "returns an empty result for out-of-range list slices."
     */
    public String read(String path, int begin, int end) {
        Node node = lookup(path);
        if (node == null) {
            // NOT defined away (§10.10): a missing file on read is real information.
            throw new ShellError("no such file: " + path);
        }
        if (node instanceof Dir) {
            throw new ShellError(path + " is a directory");
        }
        String content = ((FileNode) node).content();
        int from = clamp(begin, content.length());
        int to = clamp(end, content.length());
        if (to < from) {
            to = from; // begin past end -> empty, never an exception
        }
        return content.substring(from, to);
    }

    /**
     * Ensures no file exists at {@code path}.
     *
     * <p>The Tcl {@code unset} fix, §10.3: "it is perfectly natural for [rm] to
     * be invoked with the name of a [path] that doesn't exist. In this case, its
     * work is already done, so it can simply return."
     */
    public void rm(String path) {
        Dir parent = parentDir(path);
        if (parent == null) {
            return; // nothing to remove; the goal already holds
        }
        parent.children.remove(baseName(path)); // remove() is a no-op if absent
    }

    // --- shared, exception-free tree plumbing (identical to the `before` version) ---

    private Node lookup(String path) {
        Node cur = root;
        for (String name : segments(path)) {
            if (!(cur instanceof Dir dir)) {
                return null;
            }
            cur = dir.children.get(name);
            if (cur == null) {
                return null;
            }
        }
        return cur;
    }

    private Dir parentDir(String path) {
        List<String> seg = segments(path);
        Node cur = root;
        for (int i = 0; i < seg.size() - 1; i++) {
            if (!(cur instanceof Dir dir)) {
                return null;
            }
            cur = dir.children.get(seg.get(i));
            if (cur == null) {
                return null;
            }
        }
        return cur instanceof Dir dir ? dir : null;
    }

    private static int clamp(int i, int len) {
        return Math.max(0, Math.min(len, i));
    }

    private static List<String> segments(String path) {
        List<String> out = new ArrayList<>();
        for (String s : path.split("/")) {
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    private static String baseName(String path) {
        List<String> seg = segments(path);
        return seg.isEmpty() ? "" : seg.get(seg.size() - 1);
    }

    private static String parentPath(String path) {
        int i = path.lastIndexOf('/');
        return i <= 0 ? "/" : path.substring(0, i);
    }
}
