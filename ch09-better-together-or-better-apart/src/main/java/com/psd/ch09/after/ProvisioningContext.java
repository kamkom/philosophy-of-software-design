package com.psd.ch09.after;

/**
 * The workflow-owned data that flows between steps at runtime. It is mutable:
 * a step writes the id of a resource it created so that a <em>later</em> step
 * can use it, and so that the step's own {@code compensate} can tear that exact
 * resource down.
 *
 * <p>This type belongs to the policy layer, not to {@link Saga} — the mechanism
 * is generic in its context and never touches these fields. Keeping it here
 * (rather than baking these fields into {@code Saga}) is what lets the saga
 * stay general-purpose.
 */
public final class ProvisioningContext {
    /** Input: the tenant or workspace being provisioned. */
    public final String name;
    /** Input: the hostname to register (tenant workflow only). */
    public final String host;

    // Outputs recorded by steps as they execute:
    public String schema;
    public String bucketId;
    public String apiKeyId;
    public String seatReservationId;

    public ProvisioningContext(String name, String host) {
        this.name = name;
        this.host = host;
    }
}
