package com.psd.ch09;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simulated backing service: an in-memory object-storage bucket store.
 * {@link #create} returns the id the caller must keep in order to delete the
 * bucket again during rollback.
 */
public final class BucketStore {
    private final Map<String, String> bucketsById = new LinkedHashMap<>();

    public String create(String owner) {
        String bucketId = "bucket:" + owner;
        bucketsById.put(bucketId, owner);
        System.out.println("  bucket created:    " + bucketId);
        return bucketId;
    }

    public void delete(String bucketId) {
        bucketsById.remove(bucketId);
        System.out.println("  bucket deleted:    " + bucketId);
    }

    public Map<String, String> contents() {
        return Collections.unmodifiableMap(bucketsById);
    }
}
