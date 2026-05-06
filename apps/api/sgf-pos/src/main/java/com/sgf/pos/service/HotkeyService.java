package com.sgf.pos.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * In-memory service for keyboard shortcuts at the POS terminal.
 *
 * Supported hotkeys:
 * <pre>
 *   Ctrl+N  — New order
 *   Ctrl+B  — Barcode search focus
 *   Ctrl+P  — Process payment / complete order
 *   Ctrl+F  — Search product by name
 *   Ctrl+D  — Discount panel
 *   Ctrl+V  — Void current order
 *   Ctrl+R  — Mark order as ready
 *   F1      — Help overlay
 *   F2      — Customer lookup
 *   F3      — Prescription validation
 *   F4      — Obra social discount
 *   F5      — Print last receipt
 *   F6      — Open cash drawer
 *   F7      — Pending orders list
 *   F8      — Stock check
 *   F9      — Daily summary
 *   F10     — Branch transfer
 *   F11     — Full screen toggle
 *   F12     — Operator switch / lock screen
 *   Alt+O   — Open orders overview
 *   Escape  — Close popups / clear search
 *   Enter   — Confirm selection
 * </pre>
 *
 * This service stores the mapping and dispatches events via a listener interface.
 * The frontend (Angular/Electron) registers listeners for actual UI reactions.
 */
@Service
public class HotkeyService {

    private static final Logger log = LoggerFactory.getLogger(HotkeyService.class);

    private final ConcurrentMap<Hotkey, Set<HotkeyListener>> listeners = new ConcurrentHashMap<>();

    /**
     * Register a listener for a specific hotkey.
     */
    public void register(Hotkey hotkey, HotkeyListener listener) {
        listeners.computeIfAbsent(hotkey, k -> ConcurrentHashMap.newKeySet()).add(listener);
        log.debug("Registered listener for {}", hotkey);
    }

    /**
     * Unregister a listener.
     */
    public void unregister(Hotkey hotkey, HotkeyListener listener) {
        Set<HotkeyListener> set = listeners.get(hotkey);
        if (set != null) {
            set.remove(listener);
            log.debug("Unregistered listener for {}", hotkey);
        }
    }

    /**
     * Dispatch a hotkey event to all registered listeners.
     * Returns true if at least one listener consumed the event.
     */
    public boolean dispatch(Hotkey hotkey) {
        Set<HotkeyListener> set = listeners.get(hotkey);
        if (set == null || set.isEmpty()) {
            log.trace("No listeners for {}", hotkey);
            return false;
        }
        boolean consumed = false;
        for (HotkeyListener listener : set) {
            try {
                listener.onHotkey(hotkey);
                consumed = true;
            } catch (Exception e) {
                log.warn("Listener failed for {}: {}", hotkey, e.getMessage());
            }
        }
        return consumed;
    }

    /**
     * Parse a hotkey from a keyboard event string representation (e.g. "Ctrl+N").
     */
    public static Hotkey parse(String keyCombo) {
        if (keyCombo == null || keyCombo.isBlank()) {
            return null;
        }
        String normalized = keyCombo.trim().toUpperCase();
        // F-keys
        if (normalized.matches("F\\d{1,2}")) {
            return Hotkey.valueOf(normalized);
        }
        // Named keys
        if (normalized.equals("ESCAPE") || normalized.equals("ESC")) {
            return Hotkey.ESCAPE;
        }
        if (normalized.equals("ENTER")) {
            return Hotkey.ENTER;
        }
        // Modifier + key
        if (normalized.contains("+")) {
            String[] parts = normalized.split("\\+");
            if (parts.length != 2) return null;
            String modifier = parts[0].trim();
            String key = parts[1].trim();
            if (modifier.equals("CTRL")) {
                return switch (key) {
                    case "N" -> Hotkey.CTRL_N;
                    case "B" -> Hotkey.CTRL_B;
                    case "P" -> Hotkey.CTRL_P;
                    case "F" -> Hotkey.CTRL_F;
                    case "D" -> Hotkey.CTRL_D;
                    case "V" -> Hotkey.CTRL_V;
                    case "R" -> Hotkey.CTRL_R;
                    default -> null;
                };
            }
            if (modifier.equals("ALT")) {
                return switch (key) {
                    case "O" -> Hotkey.ALT_O;
                    default -> null;
                };
            }
        }
        return null;
    }

    // --- Types ---

    public enum Hotkey {
        CTRL_N, CTRL_B, CTRL_P, CTRL_F, CTRL_D, CTRL_V, CTRL_R,
        ALT_O,
        F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12,
        ESCAPE, ENTER;

        public String label() {
            return switch (this) {
                case CTRL_N -> "Nueva orden";
                case CTRL_B -> "Buscar código de barras";
                case CTRL_P -> "Procesar pago";
                case CTRL_F -> "Buscar producto";
                case CTRL_D -> "Panel descuentos";
                case CTRL_V -> "Anular orden";
                case CTRL_R -> "Marcar lista para pagar";
                case ALT_O  -> "Órdenes abiertas";
                case F1  -> "Ayuda";
                case F2  -> "Buscar cliente";
                case F3  -> "Validar receta";
                case F4  -> "Descuento obra social";
                case F5  -> "Reimprimir ticket";
                case F6  -> "Abrir caja";
                case F7  -> "Órdenes pendientes";
                case F8  -> "Consultar stock";
                case F9  -> "Resumen diario";
                case F10 -> "Transferencia sucursal";
                case F11 -> "Pantalla completa";
                case F12 -> "Bloquear terminal";
                case ESCAPE -> "Cerrar / Cancelar";
                case ENTER -> "Confirmar";
            };
        }
    }

    @FunctionalInterface
    public interface HotkeyListener {
        void onHotkey(Hotkey hotkey);
    }
}