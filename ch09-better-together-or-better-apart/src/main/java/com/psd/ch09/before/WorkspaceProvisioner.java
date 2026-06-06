package com.psd.ch09.before;

import com.psd.ch09.ApiKeyService;
import com.psd.ch09.SchemaRegistry;
import com.psd.ch09.SeatService;

/**
 * Provisions a workspace. A different set of steps from {@link TenantProvisioner}
 * — but look at the shape of {@link #provision}: it is the <em>same</em>
 * general-purpose mechanism copied again. The {@code boolean ...Created} flags,
 * the try/catch, the reverse-order unwind, and the shared first step
 * (create/drop schema) are all duplicated from {@code TenantProvisioner} with
 * only the special-purpose middle swapped out.
 *
 * <p>This is exactly what §9.4's Special-General Mixture predicts: trapping a
 * general-purpose mechanism inside special-purpose code forces you to re-create
 * the mechanism for every new use. A bug fix to the unwind logic (say, making
 * compensation best-effort) would have to be applied here <em>and</em> in
 * {@code TenantProvisioner} — and in every future workflow.
 */
public final class WorkspaceProvisioner {
    private final SchemaRegistry schemaRegistry;
    private final ApiKeyService apiKeyService;
    private final SeatService seatService;

    public WorkspaceProvisioner(SchemaRegistry schemaRegistry, ApiKeyService apiKeyService, SeatService seatService) {
        this.schemaRegistry = schemaRegistry;
        this.apiKeyService = apiKeyService;
        this.seatService = seatService;
    }

    public void provision(String workspace, int seats) {
        boolean schemaCreated = false;
        boolean keyIssued = false;
        String schema = workspace + "_schema";
        String apiKeyId = null;
        String reservationId = null;
        try {
            schemaRegistry.create(schema);                       // step 1 (duplicated shared step)
            schemaCreated = true;

            apiKeyId = apiKeyService.issue(workspace);           // step 2
            keyIssued = true;

            reservationId = seatService.reserve(workspace, seats); // step 3
        } catch (RuntimeException failure) {
            if (keyIssued) {
                apiKeyService.revoke(apiKeyId);
            }
            if (schemaCreated) {
                schemaRegistry.drop(schema);
            }
            // The seat reservation is the last step; if it threw, nothing after it
            // ran, so there is nothing of its own to undo here — but you have to
            // reason that out by hand, every time, for every workflow.
            throw new ProvisioningFailedException(failure);
        }
    }
}
