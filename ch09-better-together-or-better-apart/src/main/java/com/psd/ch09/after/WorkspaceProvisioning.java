package com.psd.ch09.after;

import com.psd.ch09.ApiKeyService;
import com.psd.ch09.SchemaRegistry;
import com.psd.ch09.SeatService;

/**
 * Policy layer for the workspace workflow. It reuses the same {@link Saga}
 * mechanism and the same shared {@link CreateSchemaStep} as
 * {@link TenantProvisioning}; only the special-purpose middle differs.
 *
 * <p>Adding a third workflow would be another class shaped like this one — a
 * few {@code .add(...)} calls — with no new copy of the unwind machinery. That
 * is the payoff of putting the general-purpose part in a class by itself (§9.7).
 */
public final class WorkspaceProvisioning {
    private final SchemaRegistry schemaRegistry;
    private final ApiKeyService apiKeyService;
    private final SeatService seatService;

    public WorkspaceProvisioning(SchemaRegistry schemaRegistry, ApiKeyService apiKeyService, SeatService seatService) {
        this.schemaRegistry = schemaRegistry;
        this.apiKeyService = apiKeyService;
        this.seatService = seatService;
    }

    public void provision(String workspace, int seats) {
        Saga<ProvisioningContext> saga = new Saga<ProvisioningContext>()
                .add(new CreateSchemaStep(schemaRegistry)) // same shared step as the tenant workflow
                .add(new IssueApiKeyStep(apiKeyService))
                .add(new ReserveSeatsStep(seatService, seats));
        saga.run(new ProvisioningContext(workspace, null));
    }
}
