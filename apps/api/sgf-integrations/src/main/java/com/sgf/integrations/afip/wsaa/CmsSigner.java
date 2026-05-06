package com.sgf.integrations.afip.wsaa;

public interface CmsSigner {
    String sign(String loginTicketRequestXml);
}

