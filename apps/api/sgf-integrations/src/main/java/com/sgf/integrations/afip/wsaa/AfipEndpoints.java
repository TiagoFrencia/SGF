package com.sgf.integrations.afip.wsaa;

import com.sgf.integrations.afip.service.AfipWsEnvironment;

public final class AfipEndpoints {

    private AfipEndpoints() {
    }

    public static String wsaa(AfipWsEnvironment environment) {
        return switch (environment) {
            case HOMOLOGATION -> "https://wsaahomo.afip.gov.ar/ws/services/LoginCms";
            case PRODUCTION -> "https://wsaa.afip.gov.ar/ws/services/LoginCms";
        };
    }

    public static String wsfe(AfipWsEnvironment environment) {
        return switch (environment) {
            case HOMOLOGATION -> "https://wswhomo.afip.gov.ar/wsfev1/service.asmx";
            case PRODUCTION -> "https://servicios1.afip.gov.ar/wsfev1/service.asmx";
        };
    }
}

