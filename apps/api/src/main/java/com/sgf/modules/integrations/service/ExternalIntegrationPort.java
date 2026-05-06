package com.sgf.modules.integrations.service;

public interface ExternalIntegrationPort {
    String integrationCode();
    default String status() {
        return "PENDING_IMPLEMENTATION";
    }
}

