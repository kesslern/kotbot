package us.kesslern.kotbot

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.sql.Connection
import java.sql.DriverManager

val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun main() = runBlocking {

    val url = "jdbc:sqlite:./kotbotdb"
    val connection = DriverManager.getConnection(url)

    if (!connection.hasTable("database_metadata")) {
        connection.createDatabaseMigrationTable()
        connection.createPluginDataTable()
    }

    connection.setDatabaseMetadata("test1", "foobar")
    connection.setDatabaseMetadata("test1", "foobar2")
    connection.setPluginData("test1", "test1", "foobar")
    connection.setPluginData("test1", "test1", "foobar2")

    logger.info("Metadata test1: " + connection.getDatabaseMetadata("test1"))
    logger.info("Plugin data test1: " + connection.getPluginData("test1", "test1"))

    Plugins
    KotBot.create()
}

fun Connection.hasTable(name: String): Boolean {
    val sql = "SELECT name FROM sqlite_master WHERE type='table' and name='$name'"

    this.createStatement().executeQuery(sql).use { results ->
        while (results.next()) return true
    }
    return false
}

fun Connection.setDatabaseMetadata(key: String, value: String) {
    this.createStatement().execute("REPLACE INTO database_metadata(key, value) VALUES ('$key', '$value')")
}

fun Connection.getDatabaseMetadata(key: String): String {
    val results = this.createStatement().executeQuery("SELECT value FROM database_metadata WHERE key='$key'")
    while (results.next()) {
        return results.getString("value")
    }
    return ""
}

fun Connection.setPluginData(name: String, key: String, value: String?) {
    this.createStatement().execute("REPLACE INTO plugin_data(plugin_name, key, value) VALUES ('$name', '$key', '$value')")
}

fun Connection.getPluginData(name: String, key: String): String? {
    val results = this.createStatement().executeQuery("SELECT value FROM plugin_data WHERE key='$key' AND plugin_name='$name'")
    while (results.next()) {
        return results.getString("value")
    }
    return null
}

fun Connection.createDatabaseMigrationTable() {
    this.createStatement().execute("""
        CREATE TABLE database_metadata (
            key text PRIMARY KEY NOT NULL,
            value TEXT NOT NULL
        )
    """.trimIndent())
    this.createStatement().execute("INSERT INTO database_metadata(key, value) VALUES ('version', '1')")

}

fun Connection.createPluginDataTable() {
    this.createStatement().execute("""
        CREATE TABLE plugin_data (
            plugin_name TEXT NOT NULL,
            key TEXT NOT NULL,
            value TEXT,
            CONSTRAINT PK_plugin_data PRIMARY KEY (plugin_name, key)
        );
    """.trimIndent())
}