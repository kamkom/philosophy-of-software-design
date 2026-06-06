package com.psd.ch09;

/**
 * Thrown by {@link DnsRegistry} when a hostname is already taken. This is the
 * realistic trigger for a rollback in the provisioning workflow: a later step
 * fails on pre-existing state, so the earlier steps must be undone.
 */
public class DnsConflictException extends RuntimeException {
    public DnsConflictException(String host) {
        super("hostname already registered: " + host);
    }
}
