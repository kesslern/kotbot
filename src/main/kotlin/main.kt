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