package us.kesslern.kotbot

import mu.KotlinLogging
import java.sql.Connection
import java.sql.DriverManager

private val logger = KotlinLogging.logger {}

object Database {
    val connection: Connection

    init {
        val url = "jdbc:sqlite:./kotbotdb"

        logger.info("Connecting to database: $url")
        connection = DriverManager.getConnection(url)

        if (!connection.hasTable("database_metadata")) {
            logger.info("Initializing new database")
            this.createDatabaseMigrationTable()
            this.createPluginDataTable()
        }
    }

    fun setDatabaseMetadata(key: String, value: String) {
        logger.debug("Setting database metadata: \"$key\":\"$value\"")
        connection.createStatement().execute("REPLACE INTO database_metadata(key, value) VALUES ('$key', '$value')")
    }

    fun getDatabaseMetadata(key: String): String {
        logger.debug("Getting database key: \"$key\"")
        val results = connection.createStatement().executeQuery("SELECT value FROM database_metadata WHERE key='$key'")
        while (results.next()) {
            return results.getString("value")
        }
        return ""
    }

    fun setPluginData(name: String, key: String, value: String?) {
        logger.debug("Setting plugin data for $name: \"$key\":\"$value\"")
        val statement = connection.prepareStatement("REPLACE INTO plugin_data(plugin_name, key, value) VALUES (?, ?, ?)")
        statement.setString(1, name)
        statement.setString(2, key)
        statement.setString(3, value)
        statement.executeUpdate()
    }

    fun getPluginData(name: String, key: String): String? {
        logger.debug("Getting plugin data for $name: \"$key\"")
        val results = connection.createStatement().executeQuery("SELECT value FROM plugin_data WHERE key='$key' AND plugin_name='$name'")
        while (results.next()) {
            return results.getString("value")
        }
        return null
    }

    private fun createDatabaseMigrationTable() {
        logger.trace("Creating database migration table")
        connection.createStatement().execute("""
        CREATE TABLE database_metadata (
            key text PRIMARY KEY NOT NULL,
            value TEXT NOT NULL
        )
    """.trimIndent())
        logger.trace("Inserting initial db version 1 to database_metadata table")
        connection.createStatement().execute("INSERT INTO database_metadata(key, value) VALUES ('version', '1')")

    }

    private fun createPluginDataTable() {
        logger.trace("Creating plugin data table")
        connection.createStatement().execute("""
        CREATE TABLE plugin_data (
            plugin_name TEXT NOT NULL,
            key TEXT NOT NULL,
            value TEXT,
            CONSTRAINT PK_plugin_data PRIMARY KEY (plugin_name, key)
        );
    """.trimIndent())
    }
}

fun Connection.hasTable(name: String): Boolean {
    logger.debug("Checking if table $name exists")
    val sql = "SELECT name FROM sqlite_master WHERE type='table' and name='$name'"

    this.createStatement().executeQuery(sql).use { results ->
        logger.trace("Table $name exists")
        while (results.next()) return true
    }
    logger.trace("Table $name does not exist")
    return false
}
