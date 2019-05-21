package us.kesslern.kotbot

import java.sql.Connection
import java.sql.DriverManager

object Database {
    val connection: Connection

    init {
        val url = "jdbc:sqlite:./kotbotdb"
        connection = DriverManager.getConnection(url)

        if (!connection.hasTable("database_metadata")) {
            this.createDatabaseMigrationTable()
            this.createPluginDataTable()
        }
    }

    fun setDatabaseMetadata(key: String, value: String) {
        connection.createStatement().execute("REPLACE INTO database_metadata(key, value) VALUES ('$key', '$value')")
    }

    fun getDatabaseMetadata(key: String): String {
        val results = connection.createStatement().executeQuery("SELECT value FROM database_metadata WHERE key='$key'")
        while (results.next()) {
            return results.getString("value")
        }
        return ""
    }

    fun setPluginData(name: String, key: String, value: String?) {
        val statement = connection.prepareStatement("REPLACE INTO plugin_data(plugin_name, key, value) VALUES (?, ?, ?)")
        statement.setString(1, name)
        statement.setString(2, key)
        statement.setString(3, value)
        statement.executeUpdate()
    }

    fun getPluginData(name: String, key: String): String? {
        val results = connection.createStatement().executeQuery("SELECT value FROM plugin_data WHERE key='$key' AND plugin_name='$name'")
        while (results.next()) {
            return results.getString("value")
        }
        return null
    }

    fun createDatabaseMigrationTable() {
        connection.createStatement().execute("""
        CREATE TABLE database_metadata (
            key text PRIMARY KEY NOT NULL,
            value TEXT NOT NULL
        )
    """.trimIndent())
        connection.createStatement().execute("INSERT INTO database_metadata(key, value) VALUES ('version', '1')")

    }

    fun createPluginDataTable() {
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
    val sql = "SELECT name FROM sqlite_master WHERE type='table' and name='$name'"

    this.createStatement().executeQuery(sql).use { results ->
        while (results.next()) return true
    }
    return false
}
