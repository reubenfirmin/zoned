package zoned.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import zoned.gradle.DatabaseSetup
import java.sql.Connection

// TODO this is only because flyway clean isn't working...
open class DatabaseCleaner : DefaultTask() {

    @TaskAction
    fun clean() {
        val setup = DatabaseSetup(logger)
        logger.lifecycle("CLEANING DATABASE")

        val dataSource = setup.getDataSource(project.rootDir)

        dataSource.connection.use { conn ->
            conn.autoCommit = false

            try {
                if (setup.config.dbPath != null) {
                    cleanSqlite(conn)
                } else {
                    cleanPostgres(conn)
                }

                conn.commit()
                logger.lifecycle("Database cleaned successfully")
            } catch (e: Exception) {
                logger.error("Error cleaning database", e)
                conn.rollback()
                throw e
            }
        }
    }

    private fun cleanSqlite(conn: Connection) {
        // First attempt: Try the hard reset approach
        try {
            // Turn off foreign keys
            conn.createStatement().use { it.execute("PRAGMA foreign_keys = OFF") }

            // Get all tables
            val tables = mutableListOf<String>()
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'").use { rs ->
                    while (rs.next()) {
                        tables.add(rs.getString(1))
                    }
                }
            }

            // Delete data from all tables first
            conn.createStatement().use { stmt ->
                for (table in tables) {
                    try {
                        stmt.execute("DELETE FROM \"$table\"")
                    } catch (e: Exception) {
                        logger.warn("Could not delete from table $table: ${e.message}")
                    }
                }
            }

            // Get the schema for all tables
            val tableSchemas = mutableMapOf<String, String>()
            conn.createStatement().use { stmt ->
                for (table in tables) {
                    try {
                        stmt.executeQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='$table'").use { rs ->
                            if (rs.next()) {
                                tableSchemas[table] = rs.getString(1)
                            }
                        }
                    } catch (e: Exception) {
                        logger.warn("Could not get schema for table $table: ${e.message}")
                    }
                }
            }

            // Drop all tables
            conn.createStatement().use { stmt ->
                for (table in tables) {
                    try {
                        stmt.execute("DROP TABLE IF EXISTS \"$table\"")
                    } catch (e: Exception) {
                        logger.warn("Could not drop table $table: ${e.message}")
                    }
                }
            }

            conn.commit()
        } catch (e: Exception) {
            logger.warn("First approach failed. Trying alternative method...")
            conn.rollback()

            // Second approach: Create and use a new database file
            try {
                // Create a temporary in-memory database
                conn.createStatement().use { stmt ->
                    // Attach a temporary database
                    stmt.execute("ATTACH DATABASE ':memory:' AS new_db")

                    // Export the schema (without data) to the new database
                    // This creates an empty schema structure
                    stmt.execute("SELECT sql FROM sqlite_master WHERE sql NOT NULL AND type='table' AND name NOT LIKE 'sqlite_%'")
                    stmt.executeQuery("SELECT sql FROM sqlite_master WHERE sql NOT NULL AND type='table' AND name NOT LIKE 'sqlite_%'").use { rs ->
                        while (rs.next()) {
                            val createSql = rs.getString(1)
                            // Execute the CREATE statement in the new database
                            try {
                                stmt.execute("BEGIN TRANSACTION")
                                stmt.execute(createSql.replace("CREATE TABLE ", "CREATE TABLE new_db."))
                                stmt.execute("COMMIT")
                            } catch (ex: Exception) {
                                stmt.execute("ROLLBACK")
                                logger.warn("Error creating table in new DB: ${ex.message}")
                            }
                        }
                    }

                    // Also get views, triggers, and indices
                    for (type in listOf("view", "trigger", "index")) {
                        stmt.executeQuery("SELECT sql FROM sqlite_master WHERE sql NOT NULL AND type='$type'").use { rs ->
                            while (rs.next()) {
                                val createSql = rs.getString(1)
                                try {
                                    stmt.execute(createSql)
                                } catch (ex: Exception) {
                                    logger.warn("Error creating $type in new DB: ${ex.message}")
                                }
                            }
                        }
                    }

                    // Get all tables for vacuum
                    val tables = mutableListOf<String>()
                    stmt.executeQuery("SELECT name FROM new_db.sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'").use { rs ->
                        while (rs.next()) {
                            tables.add(rs.getString(1))
                        }
                    }

                    // Turn off foreign keys and delete from all tables
                    stmt.execute("PRAGMA new_db.foreign_keys = OFF")
                    for (table in tables) {
                        try {
                            stmt.execute("DELETE FROM new_db.\"$table\"")
                        } catch (e: Exception) {
                            logger.warn("Could not delete from table $table in new DB: ${e.message}")
                        }
                    }

                    // Vacuum the new database
                    stmt.execute("VACUUM new_db")

                    // Turn on foreign keys again
                    stmt.execute("PRAGMA new_db.foreign_keys = ON")
                }
            } catch (e: Exception) {
                logger.warn("Second approach failed: ${e.message}")
                conn.rollback()

                // Last resort approach - drop all objects one by one with careful error handling
                conn.createStatement().use { stmt ->
                    stmt.execute("PRAGMA foreign_keys = OFF")

                    try {
                        // Drop views first
                        stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='view'").use { rs ->
                            while (rs.next()) {
                                val viewName = rs.getString(1)
                                try {
                                    stmt.execute("DROP VIEW IF EXISTS \"$viewName\"")
                                } catch (ex: Exception) {
                                    logger.warn("Failed to drop view $viewName: ${ex.message}")
                                }
                            }
                        }

                        // Drop triggers
                        stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='trigger'").use { rs ->
                            while (rs.next()) {
                                val triggerName = rs.getString(1)
                                try {
                                    stmt.execute("DROP TRIGGER IF EXISTS \"$triggerName\"")
                                } catch (ex: Exception) {
                                    logger.warn("Failed to drop trigger $triggerName: ${ex.message}")
                                }
                            }
                        }

                        // Drop indexes
                        stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='index' AND name NOT LIKE 'sqlite_autoindex%'").use { rs ->
                            while (rs.next()) {
                                val indexName = rs.getString(1)
                                try {
                                    stmt.execute("DROP INDEX IF EXISTS \"$indexName\"")
                                } catch (ex: Exception) {
                                    logger.warn("Failed to drop index $indexName: ${ex.message}")
                                }
                            }
                        }

                        // Try to empty all tables first, then drop them
                        val tables = mutableListOf<String>()
                        stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'").use { rs ->
                            while (rs.next()) {
                                tables.add(rs.getString(1))
                            }
                        }

                        // First pass: delete data
                        for (table in tables) {
                            try {
                                stmt.execute("DELETE FROM \"$table\"")
                            } catch (ex: Exception) {
                                logger.warn("Failed to delete from table $table: ${ex.message}")
                            }
                        }

                        // Second pass: drop tables
                        for (table in tables) {
                            try {
                                stmt.execute("DROP TABLE IF EXISTS \"$table\"")
                            } catch (ex: Exception) {
                                logger.warn("Failed to drop table $table: ${ex.message}")
                            }
                        }
                    } catch (ex: Exception) {
                        logger.warn("Final approach had errors: ${ex.message}")
                    } finally {
                        // Turn foreign keys back on
                        stmt.execute("PRAGMA foreign_keys = ON")
                    }
                }
            }
        }
    }

    private fun cleanPostgres(conn: Connection) {
        // Disable triggers temporarily
        conn.createStatement().use { it.execute("SET session_replication_role = 'replica'") }

        // Get all table names in public schema
        val tables = mutableListOf<String>()
        conn.createStatement().use { stmt ->
            stmt.executeQuery("""
                SELECT tablename FROM pg_tables 
                WHERE schemaname = 'public'
            """).use { rs ->
                while (rs.next()) {
                    tables.add(rs.getString(1))
                }
            }
        }

        // Drop all tables
        if (tables.isNotEmpty()) {
            conn.createStatement().use { stmt ->
                logger.info("Dropping tables: ${tables.joinToString()}")
                // Using CASCADE to handle dependencies
                stmt.execute("DROP TABLE IF EXISTS ${tables.joinToString(", ")} CASCADE")
            }
        }

        // Clean up sequences
        val sequences = mutableListOf<String>()
        conn.createStatement().use { stmt ->
            stmt.executeQuery("""
                SELECT sequence_name FROM information_schema.sequences
                WHERE sequence_schema = 'public'
            """).use { rs ->
                while (rs.next()) {
                    sequences.add(rs.getString(1))
                }
            }
        }

        // Drop all sequences
        if (sequences.isNotEmpty()) {
            conn.createStatement().use { stmt ->
                logger.info("Dropping sequences: ${sequences.joinToString()}")
                stmt.execute("DROP SEQUENCE IF EXISTS ${sequences.joinToString(", ")} CASCADE")
            }
        }

        // Re-enable triggers
        conn.createStatement().use { it.execute("SET session_replication_role = 'origin'") }
    }
}