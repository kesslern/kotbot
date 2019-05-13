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
        private val connection: IrcConnection
) {
    /**
     * A polyglot context for running shell commands. Does not provide any filesystem access.
     */
    private val shellContext: Context = Context.newBuilder().build()

    /**
     * An event handler for IrcConnection, which processes a ServerEvent into a KotBotEvent and passes the event to
     * each KotBotEventHandler.
     */
    private val kotBotEventCaller: suspend (ServerEvent) -> Unit = {
        if (it.command == "PRIVMSG") {
            val channel = it.parameters[0]
            val text = it.parameters[1]
            val message = KotBotEvent(channel, text)
            this.eventHandlers.forEach {
                try {
                    it(message)
                } catch (e: Exception) {
                    logger.error(e)
                }
            }
        }
    }

    /**
     * Event handlers that process KotBot events.
     */
    private val eventHandlers = mutableListOf<suspend (KotBotEvent) -> Unit>(
            {
              if (it.text.startsWith("py ")) {
                  connection.write("PRIVMSG ${IrcConfig.channel} :" + shellContext.eval("python", it.text.substring(3)))
              } else if (it.text.startsWith("js ")) {
                  connection.write("PRIVMSG ${IrcConfig.channel} :" + shellContext.eval("js", it.text.substring(3)))
              }
            }
    )

    init {
        val pluginContext = PluginContext(eventHandlerAdder = ::addEventHandler, responder = connection::write)
        Plugins.run(pluginContext)
    }

    private fun addEventHandler(handler: suspend (KotBotEvent) -> Unit) {
        eventHandlers.add(handler)
    }

    companion object {
        suspend fun create() {
            val connection = IrcConnection.create()
            val kotBot = KotBot(connection)
            connection.addEventHandler(kotBot.kotBotEventCaller)
            connection.run()
        }
    }
}