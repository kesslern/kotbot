package us.kesslern.kotbot

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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
                    val pong = "PONG ${it.parameters.first()}"
                    connection.write(pong)
                }
            },
            {
                if (it.command == "001") {
                    if (IrcConfig.identifyPassword != null) {
                        connection.write("PRIVMSG NickServ :IDENTIFY ${IrcConfig.identifyOwner} ${IrcConfig.identifyPassword}")
                    }
                    connection.write("JOIN ${IrcConfig.channel}")
                }
            },
            {
                if (it.command == "NOTICE" && !registered) {
                    registered = true
                    connection.write("NICK ${IrcConfig.username}")
                    connection.write("USER ${IrcConfig.username} 0 * :${IrcConfig.username}")
                }
            }

    )

    private suspend fun write(data: String) = connection.write(data)

    fun say(location: String, message: String) {
        GlobalScope.launch {
            write("PRIVMSG $location :$message")
        }
    }

    fun addEventHandler(eventHandler: suspend (ServerEvent) -> Unit) = eventHandlers.add(eventHandler)

    suspend fun run() {
        while (!connection.isClosedForRead()) {
            logger.info("Reading...")

            val response = MessageParser.parse(connection.readLine())
            logger.info("Server said: '$response}'")
            eventHandlers.forEach { it(response) }
        }
    }

    companion object {
        suspend fun create(): IrcConnection =
                IrcConnection(
                        RawSocketConnection.connect(
                                hostname = IrcConfig.hostname,
                                port = IrcConfig.port
                        )
                )
    }
}