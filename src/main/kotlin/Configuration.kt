package us.kesslern.kotbot

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import java.io.FileNotFoundException

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

    fun stringValue(key: String): String? {
        val value = json.string(key)
        return if (value?.isBlank() == true) null else value
    }

    fun requiredStringValue(key: String): String {
        return stringValue(key) ?: throw RuntimeException("Missing required config for $key")
    }

    fun intValue(key: String): Int? = json.int(key)

    fun requiredIntValue(key: String): Int = intValue(key) ?: throw RuntimeException("Missing required config for $key")
}

object IrcConfig {
    val username = ConfigurationFile.requiredStringValue("username")
    val hostname = ConfigurationFile.requiredStringValue("hostname")
    val identifyOwner = ConfigurationFile.stringValue("identify_owner")
    val identifyPassword = ConfigurationFile.stringValue("identify_password")
    val channel = ConfigurationFile.requiredStringValue("channel")
    val port = ConfigurationFile.requiredIntValue("port")
}
