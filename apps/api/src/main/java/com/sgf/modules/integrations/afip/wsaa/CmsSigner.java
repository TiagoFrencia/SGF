package com.sgf.modules.integrations.afip.wsaa;

public interface CmsSigner {
    String sign(String loginTicketRequestXml);
}

