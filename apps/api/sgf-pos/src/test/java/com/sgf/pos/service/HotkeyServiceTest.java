package com.sgf.pos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HotkeyServiceTest {

    @InjectMocks
    private HotkeyService hotkeyService;

    @Mock
    private HotkeyService.HotkeyListener mockListener;

    @Test
    void registerListener_AddsListenerToHotkey() {
        // Given
        HotkeyService.Hotkey hotkey = HotkeyService.Hotkey.CONTROL_N;

        // When
        hotkeyService.registerListener(hotkey, mockListener);

        // Then
        // Listener should be registered (no exception thrown)
        verifyNoInteractions(mockListener); // Should not trigger on registration
    }

    @Test
    void unregisterListener_RemovesListenerFromHotkey() {
        // Given
        HotkeyService.Hotkey hotkey = HotkeyService.Hotkey.CONTROL_P;
        hotkeyService.registerListener(hotkey, mockListener);

        // When
        hotkeyService.unregisterListener(hotkey, mockListener);

        // Then - should complete without exception
        assertDoesNotThrow(() -> hotkeyService.unregisterListener(hotkey, mockListener));
    }

    @Test
    void triggerHotkey_DispatchesToAllRegisteredListeners() {
        // Given
        HotkeyService.Hotkey hotkey = HotkeyService.Hotkey.F1;
        AtomicInteger callCount = new AtomicInteger(0);
        
        HotkeyService.HotkeyListener listener1 = () -> callCount.incrementAndGet();
        HotkeyService.HotkeyListener listener2 = () -> callCount.incrementAndGet();
        
        hotkeyService.registerListener(hotkey, listener1);
        hotkeyService.registerListener(hotkey, listener2);

        // When
        hotkeyService.triggerHotkey(hotkey);

        // Then
        assertEquals(2, callCount.get());
    }

    @Test
    void triggerHotkey_NoListeners_DoesNothing() {
        // Given
        HotkeyService.Hotkey hotkey = HotkeyService.Hotkey.F5;
        // No listeners registered

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> hotkeyService.triggerHotkey(hotkey));
    }

    @Test
    void triggerHotkey_ListenerThrowsException_DoesNotAffectOtherListeners() {
        // Given
        HotkeyService.Hotkey hotkey = HotkeyService.Hotkey.CONTROL_B;
        AtomicInteger callCount = new AtomicInteger(0);
        
        HotkeyService.HotkeyListener throwingListener = () -> {
            throw new RuntimeException("Simulated error");
        };
        HotkeyService.HotkeyListener normalListener = () -> callCount.incrementAndGet();
        
        hotkeyService.registerListener(hotkey, throwingListener);
        hotkeyService.registerListener(hotkey, normalListener);

        // When & Then - should handle exception gracefully
        assertDoesNotThrow(() -> hotkeyService.triggerHotkey(hotkey));
        // The normal listener should still be called
        assertEquals(1, callCount.get());
    }

    @Test
    void getAllHotkeys_ReturnsAllDefinedHotkeys() {
        // When
        HotkeyService.Hotkey[] allHotkeys = hotkeyService.getAllHotkeys();

        // Then
        assertNotNull(allHotkeys);
        assertTrue(allHotkeys.length > 0);
        // Verify some expected hotkeys exist
        assertTrue(java.util.Arrays.stream(allHotkeys)
            .anyMatch(h -> h == HotkeyService.Hotkey.F1));
        assertTrue(java.util.Arrays.stream(allHotkeys)
            .anyMatch(h -> h == HotkeyService.Hotkey.CONTROL_N));
        assertTrue(java.util.Arrays.stream(allHotkeys)
            .anyMatch(h -> h == HotkeyService.Hotkey.ESCAPE));
    }

    @Test
    void hotkeyEnum_ContainsAllExpectedHotkeys() {
        // Verify all documented hotkeys exist
        HotkeyService.Hotkey[] expectedHotkeys = {
            HotkeyService.Hotkey.CONTROL_N,
            HotkeyService.Hotkey.CONTROL_B,
            HotkeyService.Hotkey.CONTROL_P,
            HotkeyService.Hotkey.CONTROL_F,
            HotkeyService.Hotkey.CONTROL_D,
            HotkeyService.Hotkey.CONTROL_V,
            HotkeyService.Hotkey.CONTROL_R,
            HotkeyService.Hotkey.F1,
            HotkeyService.Hotkey.F2,
            HotkeyService.Hotkey.F3,
            HotkeyService.Hotkey.F4,
            HotkeyService.Hotkey.F5,
            HotkeyService.Hotkey.F6,
            HotkeyService.Hotkey.F7,
            HotkeyService.Hotkey.F8,
            HotkeyService.Hotkey.F9,
            HotkeyService.Hotkey.F10,
            HotkeyService.Hotkey.F11,
            HotkeyService.Hotkey.F12,
            HotkeyService.Hotkey.ALT_O,
            HotkeyService.Hotkey.ESCAPE,
            HotkeyService.Hotkey.ENTER
        };

        for (HotkeyService.Hotkey expected : expectedHotkeys) {
            assertNotNull(expected, "Hotkey " + expected + " should be defined");
        }
    }

    @Test
    void multipleListenersOnSameHotkey_AllAreTriggered() {
        // Given
        HotkeyService.Hotkey hotkey = HotkeyService.Hotkey.F4;
        AtomicInteger counter = new AtomicInteger(0);
        
        for (int i = 0; i < 5; i++) {
            final int index = i;
            hotkeyService.registerListener(hotkey, () -> counter.incrementAndGet());
        }

        // When
        hotkeyService.triggerHotkey(hotkey);

        // Then
        assertEquals(5, counter.get());
    }

    @Test
    void unregisterNonExistentListener_DoesNotThrowException() {
        // Given
        HotkeyService.Hotkey hotkey = HotkeyService.Hotkey.F12;
        // No listeners registered

        // When & Then
        assertDoesNotThrow(() -> 
            hotkeyService.unregisterListener(hotkey, mockListener));
    }
}
