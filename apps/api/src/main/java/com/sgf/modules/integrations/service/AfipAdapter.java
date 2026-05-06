package com.sgf.modules.integrations.service;

import org.springframework.stereotype.Component;

@Component
public class AfipAdapter implements ExternalIntegrationPort {
    @Override
    public String integrationCode() {
        return "AFIP";
    }
}

