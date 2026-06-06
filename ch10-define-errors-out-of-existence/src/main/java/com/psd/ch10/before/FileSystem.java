package com.psd.ch10.before;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A tiny in-memory filesystem — the {@code before} version, whose interface is
 * littered with exceptions.
 *
 * <p>Every operation that meets a slightly-unusual situation throws: deleting a
 * file that isn't there, making a directory that already exists, reading a path
 * that doesn't resolve. This is the over-defensive instinct §10.2 warns about —
 * "anything that looks even a bit suspicious is rejected with an exception,
 * which results in a proliferation of unnecessary exceptions that increase the
 * complexity of the system."
 *
 * <p>The cost lands on every caller (see {@link Shell}). Because the exceptions
 * are part of this class's interface, the class is shallow: §10.3 — "classes
 * with lots of exceptions have complex interfaces, and they are shallower than
 * classes with fewer exceptions."
 *
 * <p>The tree model and path lookup below ({@link Node}, {@link #lookup},
 * {@link #parentDir}) are identical to the {@code after} version; only the four
 * public operations differ. That isolates the lesson to the interface choices.
 */
public final class FileSystem {

    private sealed interface Node permits Dir, FileNode {}
    private static final class Dir implements Node {
        final Map<String, Node> children = new TreeMap<>();
    }
    private record FileNode(String content) implements Node {}

    private final Dir root = new Dir();

    /**
     * Creates a directory.
     *
     * <p>Throws if it already exists. With this definition mkdir "can't do its
     * job if the [directory already exists], so generating an exception makes
     * sense" — the same reasoning §10.3 calls a mistake for Tcl's {@code unset}.
     */
    public void mkdir(String path) {
        if (lookup(path) != null) {
            throw new PathExistsException(path + " already exists");
        }
        Dir parent = parentDir(path);
        if (parent == null) {
            throw new NoSuchPathException("no such directory: " + parentPath(path));
        }
        parent.children.put(baseName(path), new Dir());
    }

    /** Creates or overwrites a file. */
    public void write(String path, String data) {
        Dir parent = parentDir(path);
        if (parent == null) {
            throw new NoSuchPathException("no such directory: " + parentPath(path));
        }
        if (lookup(path) instanceof Dir) {
            throw new IsADirectoryException(path + " is a directory");
        }
        parent.children.put(baseName(path), new FileNode(data));
    }

    /**
     * Returns the <em>entire</em> contents of a file.
     *
     * <p>This is a shallow read: it cannot take a range, so a caller wanting a
     * slice must fetch the whole string and do the index arithmetic itself —
     * exactly the situation §10.5 complains about, where "a one-line method call
     * now becomes 5–10 lines of code." See {@link Shell#handleRead}.
     */
    public String read(String path) {
        Node node = lookup(path);
        if (node == null) {
            throw new NoSuchPathException("no such file: " + path);
        }
        if (node instanceof Dir) {
            throw new IsADirectoryException(path + " is a directory");
        }
        return ((FileNode) node).content();
    }

    /**
     * Deletes a file.
     *
     * <p>Throws if the path is absent. But "one of the most common uses of
     * [delete] is to clean up temporary state ... it's often hard to predict
     * exactly what state was created" (§10.3). Forcing an exception here makes
     * callers wrap every delete in a catch-and-ignore (see {@link Shell#handleRm}).
     */
    public void rm(String path) {
        Dir parent = parentDir(path);
        Node node = parent == null ? null : parent.children.get(baseName(path));
        if (node == null) {
            throw new NoSuchPathException("no such file: " + path);
        }
        parent.children.remove(baseName(path));
    }

    // --- shared, exception-free tree plumbing (identical to the `after` version) ---

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
