package com.psd.ch09;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simulated backing service: issues and revokes API keys for a workspace.
 */
public final class ApiKeyService {
    private final Map<String, String> ownerByKeyId = new LinkedHashMap<>();

    public String issue(String workspace) {
        String keyId = "key:" + workspace;
        ownerByKeyId.put(keyId, workspace);
        System.out.println("  api key issued:    " + keyId);
        return keyId;
    }

    public void revoke(String keyId) {
        ownerByKeyId.remove(keyId);
        System.out.println("  api key revoked:   " + keyId);
    }

    public Map<String, String> contents() {
        return Collections.unmodifiableMap(ownerByKeyId);
    }
}
