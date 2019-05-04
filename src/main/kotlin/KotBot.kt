package us.kesslern.kotbot

import io.ktor.util.KtorExperimentalAPI

@KtorExperimentalAPI
class KotBot private constructor(
        private val connection: IrcConnection
) {

    private val eventHandlers = mutableListOf<suspend (ServerMessage) -> Unit>(
            {
                if (it.command == "PING") {
                    val pong = "PONG ${it.parameters.first()}"
                    connection.write(pong)
                }
            },
            {
                if (it.command == "001") {
                    connection.write("JOIN #discodevs\r\n")
                    connection.write("PRIVMSG #discodevs :Hello from raw sockets in kotlin\r\n")
                }
            },
            {
                if (it.command == "NOTICE" && !registered) {
                    registered = true
                    connection.write("NICK kotbot\r\n")
                    connection.write("USER kotbot 0 * :kotbot\r\n")
                }
            }
    )

    companion object {
        suspend fun create(events: List<suspend (ServerMessage) -> Unit> = listOf()) {
            val connection = IrcConnection.connect(
                    hostname = "localhost",
                    port = 6667
            )
            val kotbot = KotBot(connection)
            kotbot.eventHandlers += events
            kotbot.run()
        }
    }

    private suspend fun run() {
        while (!connection.isClosedForRead()) {
            logger.info("Reading...")

            val response = MessageParser(connection.readLine()).parse()
            logger.info("Server said: '$response}'")
            eventHandler(response)
        }
    }

    private suspend fun eventHandler(event: ServerMessage) {
        eventHandlers.forEach { it(event) }
    }
}