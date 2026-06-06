package com.psd.ch10.after;

import com.psd.ch10.before.BeforeDemo;

/**
 * Runs the <em>same</em> script as {@link BeforeDemo} through the {@code after}
 * {@link Shell}, and prints an identical transcript — proof that defining the
 * errors away and aggregating the rest changed the code, not the behaviour.
 */
public final class AfterDemo {
    public static void main(String[] args) {
        new Shell().run(BeforeDemo.SCRIPT);
    }
}
