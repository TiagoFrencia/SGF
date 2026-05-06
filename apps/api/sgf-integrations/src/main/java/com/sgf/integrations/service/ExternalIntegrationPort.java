package com.sgf.integrations.service;

public interface ExternalIntegrationPort {
    String integrationCode();
    default String status() {
        return "PENDING_IMPLEMENTATION";
    }
}

