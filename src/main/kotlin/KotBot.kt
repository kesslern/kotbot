package us.kesslern.kotbot

import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.error
import org.graalvm.polyglot.Context

/**
 * An event that is passed to plugins and built-in bot event handlers.
 */
data class KotBotEvent (
    @JvmField val source: String,
    @JvmField val text: String
)

/**
 * The main IRC bot. Establishes a connection, joins a channel, and loads the plugins.
 */
@KtorExperimentalAPI
class KotBot private constructor(
        private val connection: RawSocketConnection
) {

    /**
     * Indicates if the IRC bot has identified and registered with the server.
     */
    private var registered = false

    /**
     * A polyglot context for running shell commands. Does not provide any filesystem access.
     */
    private val shellContext: Context = Context.newBuilder().build()

    /**
     * Event handlers that process KotBot events.
     */
    private val kotbotEventHandlers = mutableListOf<suspend (KotBotEvent) -> Unit>(
            {
              if (it.text.startsWith("py ")) {
                  connection.write("PRIVMSG ${IrcConfig.channel} :" + shellContext.eval("python", it.text.substring(3)))
              } else if (it.text.startsWith("js ")) {
                  connection.write("PRIVMSG ${IrcConfig.channel} :" + shellContext.eval("js", it.text.substring(3)))
              }
            }
    )

    /**
     * Event handlers that process events from the IRC server.
     */
    private val ircEventHandlers = listOf<suspend (ServerEvent) -> Unit>(
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
            },
            {
                if (it.command == "PRIVMSG") {
                    val channel = it.parameters[0]
                    val text = it.parameters[1]
                    val message = KotBotEvent(channel, text)
                    kotbotEventHandlers.forEach { try { it(message) } catch (e: Exception) { logger.error(e) } }
                }
            }
    )

    init {
        val pluginContext = PluginContext(eventHandlerAdder = ::addKotBotEventHandler, responder = connection::write)
        Plugins.run(pluginContext)
    }

    private suspend fun run() {
        while (!connection.isClosedForRead()) {
            logger.info("Reading...")

            val response = MessageParser.parse(connection.readLine())
            logger.info("Server said: '$response}'")
            ircEventHandlers.forEach { it(response) }
        }
    }

    private fun addKotBotEventHandler(handler: suspend (KotBotEvent) -> Unit) {
        kotbotEventHandlers.add(handler)
    }

    companion object {
        suspend fun create() {
            val connection = RawSocketConnection.connect(
                    hostname = IrcConfig.hostname,
                    port = IrcConfig.port
            )
            KotBot(connection).run()
        }
    }
}