package com.psd.ch09.before;

import com.psd.ch09.ApiKeyService;
import com.psd.ch09.BucketStore;
import com.psd.ch09.DnsRegistry;
import com.psd.ch09.SchemaRegistry;
import com.psd.ch09.SeatService;

import java.util.Set;

/**
 * Runs three scenarios against the {@code before} provisioners. Its output is
 * identical to {@link com.psd.ch09.after.AfterDemo}: this refactoring is a
 * purely structural win (§9.9 — "the best information hiding, the fewest
 * dependencies, and the deepest interfaces"), not a behavioural one.
 */
public final class BeforeDemo {
    public static void main(String[] args) {
        SchemaRegistry schemas = new SchemaRegistry();
        BucketStore buckets = new BucketStore();
        DnsRegistry dns = new DnsRegistry(Set.of("globex.example.com")); // already taken
        ApiKeyService apiKeys = new ApiKeyService();
        SeatService seats = new SeatService();

        TenantProvisioner tenants = new TenantProvisioner(schemas, buckets, dns);
        WorkspaceProvisioner workspaces = new WorkspaceProvisioner(schemas, apiKeys, seats);

        System.out.println("== Provision tenant 'acme' (succeeds) ==");
        tenants.provision("acme", "acme.example.com");

        System.out.println("== Provision workspace 'design' (succeeds, reuses schema step) ==");
        workspaces.provision("design", 25);

        System.out.println("== Provision tenant 'globex' (DNS conflict -> rollback) ==");
        try {
            tenants.provision("globex", "globex.example.com");
        } catch (ProvisioningFailedException e) {
            System.out.println("  -> failed: " + e.getCause().getMessage());
        }

        System.out.println("== Final state ==");
        System.out.println("  schemas: " + schemas.contents());
        System.out.println("  buckets: " + buckets.contents().keySet());
        System.out.println("  api keys: " + apiKeys.contents().keySet());
        System.out.println("  seats: " + seats.contents().keySet());
    }
}
