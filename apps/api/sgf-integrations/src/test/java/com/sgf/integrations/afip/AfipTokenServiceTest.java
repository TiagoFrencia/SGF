package com.sgf.integrations.afip;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sgf.integrations.afip.service.AfipProperties;
import com.sgf.integrations.afip.wsaa.AfipTokenService;
import com.sgf.integrations.afip.wsaa.CmsSigner;
import com.sgf.integrations.afip.wsaa.WsaaLoginResponse;
import com.sgf.integrations.afip.wsaa.WsaaSoapClient;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AfipTokenServiceTest {

    @Mock
    private AfipProperties properties;

    @Mock
    private CmsSigner cmsSigner;

    @Mock
    private WsaaSoapClient wsaaSoapClient;

    private AfipTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new AfipTokenService(properties, cmsSigner, wsaaSoapClient);
    }

    @Test
    void obtainsNewTokenWhenCacheIsEmpty() {
        // Arrange
        when(properties.service()).thenReturn("wsfe");
        when(properties.wsEnvironment()).thenReturn("homologacion");
        when(cmsSigner.sign(anyString())).thenReturn("CMS_SIGNED_123");
        
        WsaaLoginResponse mockResponse = new WsaaLoginResponse(
            "TOKEN_ABC123",
            "SIGN_XYZ789",
            OffsetDateTime.now().plusHours(12),
            "<xml>response</xml>"
        );
        when(wsaaSoapClient.login(anyString(), anyString())).thenReturn(mockResponse);

        // Act
        WsaaLoginResponse result = tokenService.currentToken();

        // Assert
        assertNotNull(result);
        assertEquals("TOKEN_ABC123", result.token());
        assertEquals("SIGN_XYZ789", result.sign());
        verify(wsaaSoapClient).login(anyString(), eq("CMS_SIGNED_123"));
    }

    @Test
    void returnsCachedTokenWhenNotExpired() {
        // Arrange
        when(properties.service()).thenReturn("wsfe");
        when(properties.wsEnvironment()).thenReturn("homologacion");
        when(cmsSigner.sign(anyString())).thenReturn("CMS_SIGNED_123");
        
        WsaaLoginResponse mockResponse = new WsaaLoginResponse(
            "TOKEN_CACHED",
            "SIGN_CACHED",
            OffsetDateTime.now().plusHours(10),
            "<xml>cached</xml>"
        );
        when(wsaaSoapClient.login(anyString(), anyString())).thenReturn(mockResponse);
        
        // First call to populate cache
        tokenService.currentToken();
        reset(wsaaSoapClient);

        // Act
        WsaaLoginResponse result = tokenService.currentToken();

        // Assert
        assertNotNull(result);
        assertEquals("TOKEN_CACHED", result.token());
        verify(wsaaSoapClient, never()).login(anyString(), anyString());
    }

    @Test
    void refreshesTokenWhenNearExpiration() throws InterruptedException {
        // Arrange
        when(properties.service()).thenReturn("wsfe");
        when(properties.wsEnvironment()).thenReturn("homologacion");
        when(cmsSigner.sign(anyString())).thenReturn("CMS_SIGNED_123");
        
        WsaaLoginResponse initialResponse = new WsaaLoginResponse(
            "TOKEN_EXPIRING",
            "SIGN_EXPIRING",
            OffsetDateTime.now().plusMinutes(1), // Expires in 1 minute
            "<xml>expiring</xml>"
        );
        when(wsaaSoapClient.login(anyString(), anyString())).thenReturn(initialResponse);
        
        // First call to populate cache with near-expired token
        tokenService.currentToken();
        reset(wsaaSoapClient);
        
        WsaaLoginResponse newResponse = new WsaaLoginResponse(
            "TOKEN_REFRESHED",
            "SIGN_REFRESHED",
            OffsetDateTime.now().plusHours(12),
            "<xml>refreshed</xml>"
        );
        when(wsaaSoapClient.login(anyString(), anyString())).thenReturn(newResponse);
        
        Thread.sleep(100); // Small delay to ensure time passes

        // Act
        WsaaLoginResponse result = tokenService.currentToken();

        // Assert
        assertNotNull(result);
        assertEquals("TOKEN_REFRESHED", result.token());
        verify(wsaaSoapClient).login(anyString(), anyString());
    }

    @Test
    void handlesNetworkErrorDuringTokenRefresh() {
        // Arrange
        when(properties.service()).thenReturn("wsfe");
        when(properties.wsEnvironment()).thenReturn("homologacion");
        when(cmsSigner.sign(anyString())).thenReturn("CMS_SIGNED_123");
        when(wsaaSoapClient.login(anyString(), anyString()))
            .thenThrow(new RuntimeException("Connection timeout"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> tokenService.refreshToken());
    }

    @Test
    void handlesInvalidCertificateError() {
        // Arrange
        when(properties.service()).thenReturn("wsfe");
        when(properties.wsEnvironment()).thenReturn("homologacion");
        when(cmsSigner.sign(anyString()))
            .thenThrow(new IllegalStateException("Invalid certificate format"));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> tokenService.refreshToken());
    }
}
