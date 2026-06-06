package com.psd.ch10.before;

/** Thrown by {@code mkdir} when the target path is already taken. */
public final class PathExistsException extends RuntimeException {
    public PathExistsException(String message) {
        super(message);
    }
}
