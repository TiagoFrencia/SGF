package com.sgf.integrations.etl.extract;

import com.sgf.integrations.etl.LegacyProductRecord;


/**
 * Common interface for all legacy system extractors.
 *
 * Each extractor reads from a specific legacy system format:
 * - FarmaWin: Firebird/SQL Server database
 * - Nixfarma: PostgreSQL-based pharmacy system
 * - DBF genérico: XBase/FoxPro flat files
 *
 * Extractors produce a stream of LegacyProductRecord objects
 * that flow through transform → validate → load pipeline.
 */
public interface LegacyExtractor extends AutoCloseable {

    /**
     * Human-readable name of the source system (e.g., "FarmaWin 3.5").
     */
    String sourceSystemName();

    /**
     * Open connection to the legacy data source.
     * @param connectionString Path, JDBC URL, or file path depending on extractor
     */
    void open(String connectionString);

    /**
     * Total number of records available for extraction.
     */
    long totalRecords();

    /**
     * Extract the next batch of records. Returns empty array when done.
     * Batch size is controlled by the implementation.
     */
    LegacyProductRecord[] extractBatch();

    /**
     * Check if more records are available.
     */
    boolean hasMore();

    /**
     * Reset extraction cursor to beginning.
     */
    void reset();

    /**
     * Progress percentage (0-100).
     */
    int progressPercent();

    /**
     * Close the data source and release resources.
     */
    @Override
    void close();
}