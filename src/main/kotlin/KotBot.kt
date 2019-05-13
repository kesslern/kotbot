package us.kesslern.kotbot

import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.error
import org.graalvm.polyglot.Context

/**
 * An event that is passed to plugins and built-in bot event handlers.
 */
data class KotBotEvent(
        @JvmField val message: String,
        @JvmField val source: String,
        @JvmField val command: String?,
        @JvmField val body: String?
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
            val source = it.parameters[0]
            val message = it.parameters[1]
            val splitMessage = message.split(" ")
            var command: String? = null
            var body: String? = null

            if (splitMessage[0].startsWith(IrcConfig.commandPrefix)){
                command = splitMessage[0].substring(IrcConfig.commandPrefix.length)
                body = message.substring(command.length + 1)
            }

            val event = KotBotEvent(
                    source = source,
                    message = message,
                    command = command,
                    body = body
            )

            this.eventHandlers.forEach {
                try {
                    it(event)
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
                if (it.command == "py") {
                    connection.write("PRIVMSG ${IrcConfig.channel} :" + shellContext.eval("python", it.body))
                } else if (it.command == "js") {
                    connection.write("PRIVMSG ${IrcConfig.channel} :" + shellContext.eval("js", it.body))
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