package com.sgf.integrations.service;

import org.springframework.stereotype.Component;

@Component
public class AfipAdapter implements ExternalIntegrationPort {
    @Override
    public String integrationCode() {
        return "AFIP";
    }
}

