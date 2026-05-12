package com.sgf.pos.service;

import com.sgf.core.domain.ConflictException;
import com.sgf.pos.domain.PosOrder;
import com.sgf.pos.domain.PosOrder.OrderStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Manages multiple simultaneous open orders at the POS terminal.
 *
 * A cashier can have several orders in progress:
 * - Customer A is browsing, order is DRAFT
 * - Customer B is ready to pay, order is READY
 * - Customer C just started, order is DRAFT
 *
 * Orders persist in DB (via PosOrderService) for session recovery across terminal restarts.
 * This service adds an in-memory index for fast switching between active orders.
 */
@Service
public class MultiOrderService {

    private static final Logger log = LoggerFactory.getLogger(MultiOrderService.class);

    private final PosOrderService orderService;

    /**
     * In-memory index: terminalId → list of active order IDs.
     * The actual order data lives in DB; this is just for fast lookup.
     */
    private final ConcurrentHashMap<String, List<UUID>> terminalOrders = new ConcurrentHashMap<>();

    /**
     * Which order is "active" (has focus) on each terminal.
     */
    private final ConcurrentHashMap<String, UUID> activeOrder = new ConcurrentHashMap<>();

    public MultiOrderService(PosOrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Create a new draft order on a terminal and set it as active.
     */
    public PosOrder newOrder(String terminalId, UUID branchId, String customerName,
                             String customerDocument, String notes) {
        PosOrder order = orderService.createDraft(branchId, customerName, customerDocument, notes);
        terminalOrders.compute(terminalId, (k, v) -> {
            if (v == null) v = new java.util.ArrayList<>();
            v.add(order.getId());
            return v;
        });
        activeOrder.put(terminalId, order.getId());
        log.info("Terminal {}: new order {} created, total open: {}",
                terminalId, order.getId(), countOpen(terminalId));
        return order;
    }

    /**
     * Switch active order on a terminal to a different one.
     */
    public PosOrder switchTo(String terminalId, UUID orderId) {
        if (!terminalOrders.containsKey(terminalId)
                || !terminalOrders.get(terminalId).contains(orderId)) {
            throw new ConflictException("Order " + orderId + " is not open on terminal " + terminalId);
        }
        activeOrder.put(terminalId, orderId);
        log.debug("Terminal {}: switched to order {}", terminalId, orderId);
        return orderService.findById(orderId);
    }

    /**
     * Get the currently active order for a terminal.
     */
    public Optional<PosOrder> getActive(String terminalId) {
        UUID orderId = activeOrder.get(terminalId);
        if (orderId == null) return Optional.empty();
        try {
            return Optional.of(orderService.findById(orderId));
        } catch (Exception e) {
            // Order was voided/completed externally, clean up
            activeOrder.remove(terminalId);
            terminalOrders.computeIfPresent(terminalId, (k, v) -> {
                v.remove(orderId);
                return v.isEmpty() ? null : v;
            });
            return Optional.empty();
        }
    }

    /**
     * Get the active order ID for a terminal (lighter than getActive).
     */
    public Optional<UUID> getActiveOrderId(String terminalId) {
        return Optional.ofNullable(activeOrder.get(terminalId));
    }

    /**
     * List all open orders for a terminal (DRAFT + READY).
     */
    public List<PosOrder> listOpen(String terminalId) {
        List<UUID> ids = terminalOrders.get(terminalId);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .map(orderService::findById)
                .filter(o -> o.getStatus() == OrderStatus.DRAFT || o.getStatus() == OrderStatus.READY)
                .toList();
    }

    /**
     * Remove a completed/voided order from the terminal index.
     */
    public void removeFromTerminal(String terminalId, UUID orderId) {
        terminalOrders.computeIfPresent(terminalId, (k, v) -> {
            v.remove(orderId);
            return v.isEmpty() ? null : v;
        });
        if (orderId.equals(activeOrder.get(terminalId))) {
            activeOrder.remove(terminalId);
            // Auto-switch to next open order if any
            List<UUID> remaining = terminalOrders.get(terminalId);
            if (remaining != null && !remaining.isEmpty()) {
                activeOrder.put(terminalId, remaining.get(0));
            }
        }
        log.debug("Terminal {}: order {} removed from index", terminalId, orderId);
    }

    /**
     * Count open orders on a terminal.
     */
    public int countOpen(String terminalId) {
        List<UUID> ids = terminalOrders.get(terminalId);
        return ids == null ? 0 : ids.size();
    }

    /**
     * Close all orders on a terminal (end of shift cleanup).
     */
    public void closeTerminal(String terminalId) {
        terminalOrders.remove(terminalId);
        activeOrder.remove(terminalId);
        log.info("Terminal {}: all orders cleared", terminalId);
    }

    /**
     * Recover terminal state from DB after restart.
     * Scans for all DRAFT/READY orders for the branch and indexes them.
     */
    public int recoverTerminal(String terminalId, UUID branchId) {
        List<PosOrder> openOrders = orderService.listOpenOrders(branchId);
        List<UUID> ids = openOrders.stream()
                .map(PosOrder::getId)
                .toList();
        terminalOrders.put(terminalId, ids);
        if (!ids.isEmpty()) {
            activeOrder.put(terminalId, ids.get(0));
        }
        log.info("Terminal {}: recovered {} orders", terminalId, ids.size());
        return ids.size();
    }
}
