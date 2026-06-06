package com.psd.ch09;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Simulated backing service: an in-memory set of created database schemas.
 * Like the other services here it is the shared "world" that both the
 * {@code before} and {@code after} designs act upon, and it prints each
 * mutation so the two demos produce identical, comparable traces.
 */
public final class SchemaRegistry {
    private final Set<String> schemas = new LinkedHashSet<>();

    public void create(String schema) {
        schemas.add(schema);
        System.out.println("  schema created:    " + schema);
    }

    public void drop(String schema) {
        schemas.remove(schema);
        System.out.println("  schema dropped:    " + schema);
    }

    public Set<String> contents() {
        return Collections.unmodifiableSet(schemas);
    }
}
