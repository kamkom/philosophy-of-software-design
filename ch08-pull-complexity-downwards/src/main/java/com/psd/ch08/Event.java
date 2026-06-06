package com.psd.ch08;

/**
 * One item awaiting upload (an analytics event). {@code source} identifies the
 * producer; the {@code before} demo routes each source through its own call site
 * to show how an exported exception forces <em>every</em> caller to invent a policy.
 */
public record Event(int id, String source) {
}
