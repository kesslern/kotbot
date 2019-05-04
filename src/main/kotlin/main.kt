package us.kesslern.kotbot

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

val logger = KotlinLogging.logger {}

var registered = false

@KtorExperimentalAPI
fun main() = runBlocking {
    KotBot.create()
}

@KtorExperimentalAPI
class KotBot private constructor(
        private val connection: IrcConnection
) {
    companion object {
        suspend fun create() {
            val connection = IrcConnection.connect(
                    hostname = "localhost",
                    port = 6667
            )
            val kotbot = KotBot(connection)
            kotbot.connect()
            kotbot.run()
        }
    }

    private suspend fun connect() {
        while (!connection.isClosedForRead()) {
            logger.info("Reading...")

            val response = MessageParser(connection.readLine()).parse()
            logger.info("Server said: '$response}'")
            eventHandler(connection, response)
        }
    }

    private suspend fun run() {
        while (!connection.isClosedForRead()) {
            logger.info("Reading...")

            val response = MessageParser(connection.readLine()).parse()
            logger.info("Server said: '$response}'")
            eventHandler(connection, response)
        }
    }

    private suspend fun eventHandler(connection: IrcConnection, event: ServerMessage) {
        if (event.command == "PING") {
            val pong = "PONG ${event.parameters.first()}"
            connection.write(pong)
        }
        if (event.command == "001") {
            connection.write("JOIN #discodevs\r\n")
            connection.write("PRIVMSG #discodevs :Hello from raw sockets in kotlin\r\n")
        }
        if (event.command == "NOTICE" && !registered) {
            registered = true
            connection.write("NICK kotbot\r\n")
            connection.write("USER kotbot 0 * :kotbot\r\n")
        }
    }
}

