package com.psd.ch09.after;

import com.psd.ch09.ApiKeyService;

/** Workspace step: issues an API key and can revoke it. */
public final class IssueApiKeyStep implements Step<ProvisioningContext> {
    private final ApiKeyService apiKeyService;

    public IssueApiKeyStep(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    public void execute(ProvisioningContext ctx) {
        ctx.apiKeyId = apiKeyService.issue(ctx.name);
    }

    @Override
    public void compensate(ProvisioningContext ctx) {
        apiKeyService.revoke(ctx.apiKeyId);
    }
}
