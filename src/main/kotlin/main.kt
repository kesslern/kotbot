package us.kesslern.kotbot

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.io.FileNotFoundException

val logger = KotlinLogging.logger {}

var registered = false

@KtorExperimentalAPI
fun main() = runBlocking {
    KotBot.create(listOf<suspend (ServerMessage) -> Unit>(
            {
                if (it.command == "PRIVMSG" && it.parameters[0] == "#discodevs" && it.parameters[1] == "hi") {
                    logger.info("Someone said hi")
                }
            },
            {
                if (it.command == "PRIVMSG" && it.parameters[0] == "#discodevs" && it.parameters[1] == "bye") {
                    logger.info("Someone said bye")
                }
            }
    ))
}

object IrcConfig {
    val username = ConfigurationFile.requiredStringValue("username")
    val hostname = ConfigurationFile.requiredStringValue("hostname")
    val channel = ConfigurationFile.requiredStringValue("channel")
    val port = ConfigurationFile.requiredIntValue("port")
}

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
