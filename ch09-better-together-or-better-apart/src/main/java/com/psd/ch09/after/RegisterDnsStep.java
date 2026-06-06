package com.psd.ch09.after;

import com.psd.ch09.DnsRegistry;

/**
 * Tenant step: registers a hostname. Its {@code execute} is the one that can
 * throw ({@link com.psd.ch09.DnsConflictException}); the step itself does no
 * rollback — that is the {@link Saga}'s job. The special-purpose step and the
 * general-purpose unwind are now cleanly separate.
 */
public final class RegisterDnsStep implements Step<ProvisioningContext> {
    private final DnsRegistry dnsRegistry;

    public RegisterDnsStep(DnsRegistry dnsRegistry) {
        this.dnsRegistry = dnsRegistry;
    }

    @Override
    public void execute(ProvisioningContext ctx) {
        dnsRegistry.register(ctx.host, ctx.name);
    }

    @Override
    public void compensate(ProvisioningContext ctx) {
        dnsRegistry.unregister(ctx.host);
    }
}
