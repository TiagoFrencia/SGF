package com.sgf.modules.integrations.afip.service;

public class AfipProviderException extends RuntimeException {

    private final String code;
    private final boolean retryable;
    private final String responsePayload;

    public AfipProviderException(String code, String message, boolean retryable, String responsePayload) {
        super(message);
        this.code = code;
        this.retryable = retryable;
        this.responsePayload = responsePayload;
    }

    public String getCode() {
        return code;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public String getResponsePayload() {
        return responsePayload;
    }
}

