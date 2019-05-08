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

object Configuration {
    private val json: JsonObject

    init {
        val parser: Parser = Parser.default()
        try {
            json = parser.parse("./kotbot.config.json") as JsonObject
        } catch (e: FileNotFoundException) {
            throw RuntimeException("Cannot find configuration file kotbot.config.json")
        }
    }

    fun value(key: String): String? {
        val value = json.string(key)
        return if (value?.isBlank() == true) null else value
    }
}
