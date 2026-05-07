package com.sgf.integrations.afip;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sgf.core.domain.BadRequestException;
import com.sgf.integrations.afip.service.AfipConnectivityService;
import com.sgf.integrations.afip.service.AfipMode;
import com.sgf.integrations.afip.service.AfipProperties;
import com.sgf.integrations.afip.service.AfipWsEnvironment;
import com.sgf.integrations.afip.web.AfipHealthResponse;
import com.sgf.integrations.afip.wsaa.AfipTokenService;
import com.sgf.integrations.afip.wsaa.WsaaLoginResponse;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AfipConnectivityTest {

    @Mock
    private AfipProperties properties;

    @Mock
    private AfipTokenService tokenService;

    private AfipConnectivityService connectivityService;

    @BeforeEach
    void setUp() {
        connectivityService = new AfipConnectivityService(properties, tokenService);
    }

    @Test
    void reportsDisabledWhenIntegrationNotEnabled() {
        // Arrange
        when(properties.enabled()).thenReturn(false);
        when(properties.mode()).thenReturn(AfipMode.PRODUCTION);
        when(properties.wsEnvironment()).thenReturn(AfipWsEnvironment.PRODUCTION);
        when(properties.service()).thenReturn("wsfe");
        when(properties.cuit()).thenReturn("20123456789");
        when(properties.pointOfSale()).thenReturn(1);
        when(properties.pkcs12Path()).thenReturn(null);
        when(properties.certificatePath()).thenReturn(null);
        when(properties.privateKeyPath()).thenReturn(null);

        // Act
        AfipHealthResponse response = connectivityService.inspect(false);

        // Assert
        assertNotNull(response);
        assertFalse(response.enabled());
        assertEquals("AFIP integration is disabled", response.message());
        verify(tokenService, never()).currentToken();
        verify(tokenService, never()).refreshToken();
    }

    @Test
    void reportsHealthyInSandboxMode() {
        // Arrange
        when(properties.enabled()).thenReturn(true);
        when(properties.mode()).thenReturn(AfipMode.SANDBOX);
        when(properties.wsEnvironment()).thenReturn(AfipWsEnvironment.HOMOLOGACION);
        when(properties.service()).thenReturn("wsfe");
        when(properties.cuit()).thenReturn("20123456789");
        when(properties.pointOfSale()).thenReturn(1);
        when(properties.pkcs12Path()).thenReturn("/path/cert.p12");
        when(properties.certificatePath()).thenReturn(null);
        when(properties.privateKeyPath()).thenReturn(null);

        // Act
        AfipHealthResponse response = connectivityService.inspect(false);

        // Assert
        assertNotNull(response);
        assertTrue(response.enabled());
        assertEquals(AfipMode.SANDBOX, response.mode());
        assertEquals("Sandbox mode does not require WSAA token validation", response.message());
        verify(tokenService, never()).currentToken();
        verify(tokenService, never()).refreshToken();
    }

    @Test
    void reportsHealthyInProductionModeWithValidToken() {
        // Arrange
        when(properties.enabled()).thenReturn(true);
        when(properties.mode()).thenReturn(AfipMode.PRODUCTION);
        when(properties.wsEnvironment()).thenReturn(AfipWsEnvironment.PRODUCTION);
        when(properties.service()).thenReturn("wsfe");
        when(properties.cuit()).thenReturn("20123456789");
        when(properties.pointOfSale()).thenReturn(1);
        when(properties.pkcs12Path()).thenReturn("/path/cert.p12");
        when(properties.certificatePath()).thenReturn(null);
        when(properties.privateKeyPath()).thenReturn(null);
        
        WsaaLoginResponse mockToken = new WsaaLoginResponse(
            "TOKEN_PROD",
            "SIGN_PROD",
            OffsetDateTime.now().plusHours(10),
            "<xml>token</xml>"
        );
        when(tokenService.currentToken()).thenReturn(mockToken);

        // Act
        AfipHealthResponse response = connectivityService.inspect(false);

        // Assert
        assertNotNull(response);
        assertTrue(response.enabled());
        assertEquals(AfipMode.PRODUCTION, response.mode());
        assertTrue(response.tokenAvailable());
        assertEquals("WSAA token acquired successfully", response.message());
        assertEquals("PKCS12", response.certificateStrategy());
        verify(tokenService).currentToken();
    }

    @Test
    void forcesTokenRefreshWhenRequested() {
        // Arrange
        when(properties.enabled()).thenReturn(true);
        when(properties.mode()).thenReturn(AfipMode.PRODUCTION);
        when(properties.wsEnvironment()).thenReturn(AfipWsEnvironment.PRODUCTION);
        when(properties.service()).thenReturn("wsfe");
        when(properties.cuit()).thenReturn("20123456789");
        when(properties.pointOfSale()).thenReturn(1);
        when(properties.pkcs12Path()).thenReturn(null);
        when(properties.certificatePath()).thenReturn("/path/cert.pem");
        when(properties.privateKeyPath()).thenReturn("/path/key.pem");
        
        WsaaLoginResponse mockToken = new WsaaLoginResponse(
            "TOKEN_REFRESHED",
            "SIGN_REFRESHED",
            OffsetDateTime.now().plusHours(12),
            "<xml>refreshed</xml>"
        );
        when(tokenService.refreshToken()).thenReturn(mockToken);

        // Act
        AfipHealthResponse response = connectivityService.inspect(true);

        // Assert
        assertNotNull(response);
        assertTrue(response.tokenAvailable());
        assertEquals("TOKEN_REFRESHED", response.tokenExpiresAt().toString(), "Token should be refreshed");
        verify(tokenService, never()).currentToken();
        verify(tokenService).refreshToken();
    }

    @Test
    void throwsErrorWhenProductionModeWithoutCredentials() {
        // Arrange
        when(properties.enabled()).thenReturn(true);
        when(properties.mode()).thenReturn(AfipMode.PRODUCTION);
        when(properties.wsEnvironment()).thenReturn(AfipWsEnvironment.PRODUCTION);
        when(properties.service()).thenReturn("wsfe");
        when(properties.cuit()).thenReturn("20123456789");
        when(properties.pointOfSale()).thenReturn(1);
        when(properties.pkcs12Path()).thenReturn(null);
        when(properties.certificatePath()).thenReturn(null);
        when(properties.privateKeyPath()).thenReturn(null);

        // Act & Assert
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> connectivityService.inspect(false)
        );
        assertTrue(exception.getMessage().contains("requires PKCS12 or certificate/private key paths"));
    }

    @Test
    void reportsPemCertificateStrategyWhenUsingPemPair() {
        // Arrange
        when(properties.enabled()).thenReturn(true);
        when(properties.mode()).thenReturn(AfipMode.PRODUCTION);
        when(properties.wsEnvironment()).thenReturn(AfipWsEnvironment.PRODUCTION);
        when(properties.service()).thenReturn("wsfe");
        when(properties.cuit()).thenReturn("20123456789");
        when(properties.pointOfSale()).thenReturn(1);
        when(properties.pkcs12Path()).thenReturn(null);
        when(properties.certificatePath()).thenReturn("/path/cert.pem");
        when(properties.privateKeyPath()).thenReturn("/path/key.pem");
        
        WsaaLoginResponse mockToken = new WsaaLoginResponse(
            "TOKEN_PEM",
            "SIGN_PEM",
            OffsetDateTime.now().plusHours(8),
            "<xml>token</xml>"
        );
        when(tokenService.currentToken()).thenReturn(mockToken);

        // Act
        AfipHealthResponse response = connectivityService.inspect(false);

        // Assert
        assertNotNull(response);
        assertEquals("PEM_PAIR", response.certificateStrategy());
    }

    @Test
    void reportsUnconfiguredCertificateStrategy() {
        // Arrange - Sandbox mode doesn't validate credentials
        when(properties.enabled()).thenReturn(true);
        when(properties.mode()).thenReturn(AfipMode.SANDBOX);
        when(properties.wsEnvironment()).thenReturn(AfipWsEnvironment.HOMOLOGACION);
        when(properties.service()).thenReturn("wsfe");
        when(properties.cuit()).thenReturn("20123456789");
        when(properties.pointOfSale()).thenReturn(1);
        when(properties.pkcs12Path()).thenReturn(null);
        when(properties.certificatePath()).thenReturn(null);
        when(properties.privateKeyPath()).thenReturn(null);

        // Act
        AfipHealthResponse response = connectivityService.inspect(false);

        // Assert
        assertNotNull(response);
        assertTrue(response.enabled());
        assertEquals("UNCONFIGURED", response.certificateStrategy());
    }
}
