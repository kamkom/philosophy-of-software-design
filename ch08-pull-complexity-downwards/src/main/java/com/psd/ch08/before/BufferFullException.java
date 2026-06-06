package com.psd.ch08.before;

/**
 * Thrown by {@link BatchingUploader#offer} when the buffer is full.
 *
 * <p>This is complexity pushed <em>upward</em>: the uploader hit a condition it
 * "wasn't certain how to deal with", so "the easiest thing is to throw an exception
 * and let the caller handle it" (§8 intro). The cost lands on everyone else —
 * "if a class throws an exception, every caller of the class will have to deal with
 * it" (§8 intro) — and, as the demo shows, each caller deals with it differently.
 */
public final class BufferFullException extends RuntimeException {

    public BufferFullException(int bufferSize) {
        super("upload buffer full at " + bufferSize + " events");
    }
}
