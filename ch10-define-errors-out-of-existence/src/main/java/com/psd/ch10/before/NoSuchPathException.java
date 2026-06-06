package com.psd.ch10.before;

/** Thrown when a path does not resolve to an existing file or directory. */
public final class NoSuchPathException extends RuntimeException {
    public NoSuchPathException(String message) {
        super(message);
    }
}
