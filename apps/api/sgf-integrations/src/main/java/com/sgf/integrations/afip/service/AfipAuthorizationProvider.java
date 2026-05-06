package com.sgf.integrations.afip.service;

public interface AfipAuthorizationProvider {
    AfipMode mode();
    AfipAuthorizationResult authorize(AfipAuthorizationCommand command);
}

