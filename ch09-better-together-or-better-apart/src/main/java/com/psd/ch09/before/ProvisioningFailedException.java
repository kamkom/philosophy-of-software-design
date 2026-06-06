package com.psd.ch09.before;

/** Raised after a provisioning workflow has failed and been rolled back. */
public class ProvisioningFailedException extends RuntimeException {
    public ProvisioningFailedException(Throwable cause) {
        super("provisioning failed and was rolled back", cause);
    }
}
