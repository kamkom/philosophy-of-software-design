package com.psd.ch09.after;

import com.psd.ch09.ApiKeyService;
import com.psd.ch09.BucketStore;
import com.psd.ch09.DnsRegistry;
import com.psd.ch09.SchemaRegistry;
import com.psd.ch09.SeatService;

import java.util.Set;

/**
 * Same three scenarios as {@link com.psd.ch09.before.BeforeDemo}, and the same
 * output — proving the change is structural, not behavioural.
 */
public final class AfterDemo {
    public static void main(String[] args) {
        SchemaRegistry schemas = new SchemaRegistry();
        BucketStore buckets = new BucketStore();
        DnsRegistry dns = new DnsRegistry(Set.of("globex.example.com")); // already taken
        ApiKeyService apiKeys = new ApiKeyService();
        SeatService seats = new SeatService();

        TenantProvisioning tenants = new TenantProvisioning(schemas, buckets, dns);
        WorkspaceProvisioning workspaces = new WorkspaceProvisioning(schemas, apiKeys, seats);

        System.out.println("== Provision tenant 'acme' (succeeds) ==");
        tenants.provision("acme", "acme.example.com");

        System.out.println("== Provision workspace 'design' (succeeds, reuses schema step) ==");
        workspaces.provision("design", 25);

        System.out.println("== Provision tenant 'globex' (DNS conflict -> rollback) ==");
        try {
            tenants.provision("globex", "globex.example.com");
        } catch (SagaFailedException e) {
            System.out.println("  -> failed: " + e.getCause().getMessage());
        }

        System.out.println("== Final state ==");
        System.out.println("  schemas: " + schemas.contents());
        System.out.println("  buckets: " + buckets.contents().keySet());
        System.out.println("  api keys: " + apiKeys.contents().keySet());
        System.out.println("  seats: " + seats.contents().keySet());
    }
}
