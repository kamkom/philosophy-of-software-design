package com.psd.ch09.before;

import com.psd.ch09.BucketStore;
import com.psd.ch09.DnsRegistry;
import com.psd.ch09.SchemaRegistry;

/**
 * Provisions a tenant by running three steps in order, rolling everything back
 * if any step fails.
 *
 * <p>The trouble is buried in {@link #provision}: alongside the tenant-specific
 * business logic (which services to call, in which order) this method also
 * hand-rolls a <em>general-purpose</em> mechanism — "remember which steps
 * completed, and on failure undo them in reverse order." That mechanism has
 * nothing to do with tenants. §9.4: a module's general-purpose mechanism
 * "should not include code that specializes the mechanism for a particular
 * use." Here it is the other way round — a specialized use has swallowed the
 * mechanism — which is the Special-General Mixture red flag: "a general-purpose
 * mechanism also contains code specialized for a particular use of that
 * mechanism ... creates information leakage between the mechanism and the
 * particular use case." (§9.4)
 *
 * <p>Because the mechanism lives inside this class, {@link WorkspaceProvisioner}
 * has to copy the entire bookkeeping-and-unwind skeleton — and the shared first
 * step (create/drop schema) — verbatim. §9.3 Red Flag (Repetition): "If the
 * same piece of code (or code that is almost the same) appears over and over
 * again, that's a red flag that you haven't found the right abstractions."
 */
public final class TenantProvisioner {
    private final SchemaRegistry schemaRegistry;
    private final BucketStore bucketStore;
    private final DnsRegistry dnsRegistry;

    public TenantProvisioner(SchemaRegistry schemaRegistry, BucketStore bucketStore, DnsRegistry dnsRegistry) {
        this.schemaRegistry = schemaRegistry;
        this.bucketStore = bucketStore;
        this.dnsRegistry = dnsRegistry;
    }

    public void provision(String tenant, String host) {
        // --- general-purpose rollback bookkeeping, tangled into tenant logic ---
        boolean schemaCreated = false;
        boolean bucketCreated = false;
        String schema = tenant + "_schema";
        String bucketId = null;
        try {
            schemaRegistry.create(schema);          // step 1 (shared with WorkspaceProvisioner)
            schemaCreated = true;

            bucketId = bucketStore.create(tenant);  // step 2
            bucketCreated = true;

            dnsRegistry.register(host, tenant);     // step 3 — may throw DnsConflictException
        } catch (RuntimeException failure) {
            // Hand-coded "undo completed steps in reverse." Note the order is the
            // reverse of creation and must be kept in sync by hand with the block
            // above — easy to get wrong, and duplicated in every workflow class.
            if (bucketCreated) {
                bucketStore.delete(bucketId);
            }
            if (schemaCreated) {
                schemaRegistry.drop(schema);
            }
            throw new ProvisioningFailedException(failure);
        }
    }
}
