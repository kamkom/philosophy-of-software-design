package com.psd.ch09;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Simulated backing service: an in-memory DNS table. Registering a hostname
 * that is already taken throws {@link DnsConflictException}; this is how the
 * rollback scenario is triggered without any artificial "fail on step N" hook.
 */
public final class DnsRegistry {
    private final Map<String, String> targetsByHost = new LinkedHashMap<>();

    /** Pre-occupied hostnames seeded silently, so a later workflow can collide. */
    public DnsRegistry(Set<String> alreadyTaken) {
        for (String host : alreadyTaken) {
            targetsByHost.put(host, "<pre-existing>");
        }
    }

    public void register(String host, String target) {
        if (targetsByHost.containsKey(host)) {
            throw new DnsConflictException(host);
        }
        targetsByHost.put(host, target);
        System.out.println("  dns registered:    " + host + " -> " + target);
    }

    public void unregister(String host) {
        targetsByHost.remove(host);
        System.out.println("  dns unregistered:  " + host);
    }

    public Map<String, String> contents() {
        return Collections.unmodifiableMap(targetsByHost);
    }
}
