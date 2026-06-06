package com.psd.ch09.after;

import com.psd.ch09.SchemaRegistry;

/**
 * Shared first step of both workflows. In the {@code before} design the
 * create-then-drop logic was copied into both provisioner classes; here it
 * exists exactly once and both assemblers reuse this instance. (§9.3 — the
 * Repetition red flag is gone.)
 */
public final class CreateSchemaStep implements Step<ProvisioningContext> {
    private final SchemaRegistry schemaRegistry;

    public CreateSchemaStep(SchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    @Override
    public void execute(ProvisioningContext ctx) {
        ctx.schema = ctx.name + "_schema";
        schemaRegistry.create(ctx.schema);
    }

    @Override
    public void compensate(ProvisioningContext ctx) {
        schemaRegistry.drop(ctx.schema);
    }
}
