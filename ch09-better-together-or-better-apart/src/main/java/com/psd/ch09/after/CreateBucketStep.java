package com.psd.ch09.after;

import com.psd.ch09.BucketStore;

/** Tenant step: creates an object-storage bucket and can delete it again. */
public final class CreateBucketStep implements Step<ProvisioningContext> {
    private final BucketStore bucketStore;

    public CreateBucketStep(BucketStore bucketStore) {
        this.bucketStore = bucketStore;
    }

    @Override
    public void execute(ProvisioningContext ctx) {
        ctx.bucketId = bucketStore.create(ctx.name); // record id for later compensation
    }

    @Override
    public void compensate(ProvisioningContext ctx) {
        bucketStore.delete(ctx.bucketId);
    }
}
