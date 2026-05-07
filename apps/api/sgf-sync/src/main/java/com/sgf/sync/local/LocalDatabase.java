package com.sgf.sync.local;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Manages the local SQLite database for offline-first operations.
 * Mirrors the PostgreSQL schema in a lightweight embedded DB.
 */
public class LocalDatabase {

    private final String dbUrl;

    public LocalDatabase() {
        this("jdbc:sqlite:sgf-local.db");
    }

    public LocalDatabase(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite driver not found", e);
        }
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(dbUrl);
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to local SQLite DB", e);
        }
    }

    public void initialize() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA foreign_keys=ON");

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS local_products (
                        id TEXT PRIMARY KEY,
                        gtin TEXT NOT NULL,
                        sku TEXT NOT NULL,
                        commercial_name TEXT NOT NULL,
                        brand TEXT,
                        active_ingredient TEXT,
                        prescription_required INTEGER DEFAULT 0,
                        updated_at TEXT NOT NULL,
                        synced INTEGER DEFAULT 0
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS local_sales (
                        id TEXT PRIMARY KEY,
                        external_idempotency_key TEXT NOT NULL UNIQUE,
                        total_amount REAL NOT NULL,
                        status TEXT NOT NULL,
                        sold_at TEXT NOT NULL,
                        synced INTEGER DEFAULT 0,
                        updated_at TEXT NOT NULL
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS local_sync_queue (
                        id TEXT PRIMARY KEY,
                        aggregate_type TEXT NOT NULL,
                        aggregate_id TEXT,
                        event_type TEXT NOT NULL,
                        payload_json TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        created_at TEXT NOT NULL,
                        processed_at TEXT,
                        retries INTEGER DEFAULT 0,
                        last_error TEXT
                    )
                    """);

            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS idx_local_sync_status
                    ON local_sync_queue(status, created_at)
                    """);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize local SQLite DB", e);
        }
    }
}