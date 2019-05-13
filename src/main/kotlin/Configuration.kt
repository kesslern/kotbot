package us.kesslern.kotbot

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import java.io.FileNotFoundException

/**
 * Convenience object for loading the KotBot config and retrieving values.
 */
object ConfigurationFile {
    private val json: JsonObject

    init {
        val parser: Parser = Parser.default()
        try {
            json = parser.parse("./kotbot.config.json") as JsonObject
        } catch (e: FileNotFoundException) {
            throw RuntimeException("Cannot find configuration file kotbot.config.json")
        }
    }

    /**
     * Retrieve a string value by key. If the key does not exist or the value is blank, `null` is returned.
     */
    fun stringValue(key: String): String? {
        val value = json.string(key)
        return if (value?.isBlank() == true) null else value
    }

    /**
     * Retrieve a string value by key. If the key does nto exist or the value is blank, a RuntimeException is thrown.
     */
    fun requiredStringValue(key: String): String {
        return stringValue(key) ?: throw RuntimeException("Missing required config for $key")
    }

    fun intValue(key: String): Int? = json.int(key)

    fun requiredIntValue(key: String): Int = intValue(key) ?: throw RuntimeException("Missing required config for $key")
}

/**
 * All configuration values used for KotBot and its IRC configuration.
 */
object IrcConfig {
    val username = ConfigurationFile.requiredStringValue("username")
    val hostname = ConfigurationFile.requiredStringValue("hostname")
    val identifyOwner = ConfigurationFile.stringValue("identify_owner")
    val identifyPassword = ConfigurationFile.stringValue("identify_password")
    val channel = ConfigurationFile.requiredStringValue("channel")
    val port = ConfigurationFile.requiredIntValue("port")
}
