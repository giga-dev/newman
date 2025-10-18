package com.gigaspaces.newman.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Database migration component that runs on application startup.
 * Handles schema modifications that cannot be expressed through JPA annotations.
 */
@Component
public class DatabaseMigration {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigration.class);
    private final AtomicBoolean migrated = new AtomicBoolean(false);

    @Autowired
    private DataSource dataSource;

    @EventListener(ContextRefreshedEvent.class)
    public void migrate() {
        // Ensure migration only runs once
        if (!migrated.compareAndSet(false, true)) {
            return;
        }
        logger.info("Starting database migration...");

        try (Connection conn = dataSource.getConnection()) {
            migrateSuiteForeignKey(conn);
            logger.info("Database migration completed successfully");
        } catch (Exception e) {
            logger.error("Database migration failed", e);
            // Don't throw exception - let the app start even if migration fails
        }
    }

    /**
     * Migrates the job.suite_id foreign key to allow ON DELETE SET NULL.
     * This allows Suite entities to be deleted while preserving Job records with denormalized suite info.
     * Note: suite_name column is created automatically by Hibernate based on Job entity definition.
     */
    private void migrateSuiteForeignKey(Connection conn) throws Exception {
        logger.info("Migrating job.suite_id foreign key constraint...");

        try (Statement stmt = conn.createStatement()) {

            // Step 1: Populate suite_name from suite table for existing jobs
            logger.info("Populating suite_name from suite table for existing jobs...");
            int updatedRows = stmt.executeUpdate(
                    "UPDATE job SET suite_name = (" +
                    "  SELECT name FROM suite WHERE suite.id = job.suite_id" +
                    ") WHERE suite_id IS NOT NULL AND suite_name IS NULL");
            logger.info("Populated suite_name for {} jobs", updatedRows);

            // Step 2: Find existing foreign key constraint
            String constraintName = null;
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT constraint_name FROM information_schema.table_constraints " +
                    "WHERE table_name = 'job' AND constraint_type = 'FOREIGN KEY' " +
                    "AND constraint_name LIKE '%suite%'")) {
                if (rs.next()) {
                    constraintName = rs.getString("constraint_name");
                }
            }

            if (constraintName == null) {
                logger.warn("No suite foreign key constraint found - might already be migrated or doesn't exist");
                return;
            }

            logger.info("Found existing constraint: {}", constraintName);

            // Step 3: Check if constraint already has ON DELETE SET NULL
            boolean needsMigration = true;
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT rc.delete_rule " +
                    "FROM information_schema.referential_constraints rc " +
                    "JOIN information_schema.table_constraints tc " +
                    "  ON rc.constraint_name = tc.constraint_name " +
                    "WHERE tc.table_name = 'job' AND tc.constraint_name = '" + constraintName + "'")) {
                if (rs.next()) {
                    String deleteRule = rs.getString("delete_rule");
                    if ("SET NULL".equals(deleteRule)) {
                        logger.info("Foreign key already has ON DELETE SET NULL, skipping migration");
                        needsMigration = false;
                    }
                }
            }

            if (!needsMigration) {
                return;
            }

            // Step 4: Drop old constraint
            logger.info("Dropping old constraint: {}", constraintName);
            stmt.execute("ALTER TABLE job DROP CONSTRAINT " + constraintName);

            // Step 5: Recreate with ON DELETE SET NULL
            logger.info("Creating new constraint with ON DELETE SET NULL");
            stmt.execute(
                    "ALTER TABLE job ADD CONSTRAINT fk_job_suite " +
                    "FOREIGN KEY (suite_id) REFERENCES suite(id) ON DELETE SET NULL");

            logger.info("Suite foreign key migration completed successfully");

        } catch (Exception e) {
            logger.error("Failed to migrate suite foreign key", e);
            throw e;
        }
    }
}
