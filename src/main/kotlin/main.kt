package us.kesslern.kotbot

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

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
