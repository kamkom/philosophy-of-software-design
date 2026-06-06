package com.psd.ch10.before;

/** Thrown when an operation expects a file but the path names a directory. */
public final class IsADirectoryException extends RuntimeException {
    public IsADirectoryException(String message) {
        super(message);
    }
}
