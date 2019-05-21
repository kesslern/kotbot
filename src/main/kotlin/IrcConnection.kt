package us.kesslern.kotbot

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
class IrcConnection(
        private val connection: RawSocketConnection
) {
    /**
     * Indicates if the IRC connection has identified and registered with the server.
     */
    private var registered = false

    /**
     * Event handlers that process events from the IRC server.
     */
    private val eventHandlers = mutableListOf<suspend (ServerEvent) -> Unit>(
            {
                if (it.command == "PING") {
                    logger.info("PING from server ${it.parameters.first()}")
                    val pong = "PONG ${it.parameters.first()}"
                    connection.write(pong)
                }
            },
            {
                if (it.command == "001") {
                    if (IrcConfig.identifyPassword != null) {
                        logger.info("Identifying to services as ${IrcConfig.identifyOwner}")
                        connection.write("PRIVMSG NickServ :IDENTIFY ${IrcConfig.identifyOwner} ${IrcConfig.identifyPassword}")
                    }
                    logger.info("Joining ${IrcConfig.channel}")
                    connection.write("JOIN ${IrcConfig.channel}")
                }
            },
            {
                if (it.command == "NOTICE" && !registered) {
                    registered = true
                    logger.info("Registering with server as ${IrcConfig.username}")
                    connection.write("NICK ${IrcConfig.username}")
                    connection.write("USER ${IrcConfig.username} 0 * :${IrcConfig.username}")
                }
            }

    )

    private fun write(data: String) {
        logger.trace("Writing data to connection: $data")
        runBlocking {
            connection.write(data)
        }
    }

    fun say(location: String, message: String) = write("PRIVMSG $location :$message")

    fun addEventHandler(eventHandler: suspend (ServerEvent) -> Unit) {
        eventHandlers.add(eventHandler)
    }

    fun run() {
        logger.debug("Starting IrcConnection read loop...")
        while (!connection.isClosedForRead()) {

            runBlocking {
                logger.debug("Waiting for message from server...")
                val response = MessageParser.parse(connection.readLine())

                logger.trace("Got message from server: '$response}'")
                eventHandlers.forEach { it(response) }
            }
        }
    }

    companion object {
        suspend fun create(): IrcConnection {
            logger.info("Connecting to ${IrcConfig.hostname} port ${IrcConfig.port}")
            return IrcConnection(
                    RawSocketConnection.connect(
                            hostname = IrcConfig.hostname,
                            port = IrcConfig.port
                    )
            )
        }
    }
}