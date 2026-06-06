package com.psd.ch09.after;

import com.psd.ch09.BucketStore;
import com.psd.ch09.DnsRegistry;
import com.psd.ch09.SchemaRegistry;

/**
 * Policy layer for the tenant workflow: it decides <em>which</em> steps run and
 * <em>in what order</em>, then hands them to the general-purpose {@link Saga}.
 * This is the third of §9.7's three categories — "the policy for grouping
 * actions ... implemented by high-level code" — kept separate from both the
 * mechanism ({@link Saga}) and the specifics (the {@link Step}s).
 *
 * <p>Note how thin this is: no rollback bookkeeping, no try/catch, no reverse
 * unwind. All of that moved down into {@code Saga}. Compare the
 * {@code before} provisioners, where that machinery dominated the class.
 */
public final class TenantProvisioning {
    private final SchemaRegistry schemaRegistry;
    private final BucketStore bucketStore;
    private final DnsRegistry dnsRegistry;

    public TenantProvisioning(SchemaRegistry schemaRegistry, BucketStore bucketStore, DnsRegistry dnsRegistry) {
        this.schemaRegistry = schemaRegistry;
        this.bucketStore = bucketStore;
        this.dnsRegistry = dnsRegistry;
    }

    public void provision(String tenant, String host) {
        Saga<ProvisioningContext> saga = new Saga<ProvisioningContext>()
                .add(new CreateSchemaStep(schemaRegistry)) // shared step, reused as-is
                .add(new CreateBucketStep(bucketStore))
                .add(new RegisterDnsStep(dnsRegistry));
        saga.run(new ProvisioningContext(tenant, host));
    }
}
