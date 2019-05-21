package us.kesslern.kotbot

import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.error
import kotlinx.coroutines.runBlocking
import org.graalvm.polyglot.Context

/**
 * An event that is passed to plugins and built-in bot event handlers.
 */
data class KotBotEvent(
        @JvmField val message: String,
        @JvmField val source: String,
        @JvmField val command: String?,
        @JvmField val body: String?,
        @JvmField val name: String,
        @JvmField val sayer: (String) -> Unit,
        @JvmField val responder: (String) -> Unit
) {
    fun say(message: String) {
        sayer(message)
    }

    fun respond(message: String) {
        responder(message)
    }
}

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

    private val helpInfo = mutableMapOf<String, String>()

    /**
     * An event handler for IrcConnection, which processes a ServerEvent into a KotBotEvent and passes the event to
     * each KotBotEventHandler.
     */
    private val kotBotEventCaller: suspend (ServerEvent) -> Unit = { event ->
        if (event.command == "PRIVMSG") {
            val source = event.parameters[0]
            val message = event.parameters[1]
            val splitMessage = message.split(" ")
            var command: String? = null
            var body: String? = null

            if (splitMessage[0].startsWith(IrcConfig.commandPrefix)){
                command = splitMessage[0].substring(IrcConfig.commandPrefix.length)
                body = if (message.length > command.length + 1) {
                    message.substring(command.length + 2)
                } else ""
            }

            val kotBotEvent = KotBotEvent(
                    source = source,
                    message = message,
                    command = command,
                    body = body,
                    name = event.name!!,
                    sayer = {
                        runBlocking {
                            connection.say(source, it)
                        }
                    },
                    responder = {
                        val privateMessage = source == IrcConfig.username
                        val location = if (privateMessage) event.name!! else source
                        val prefix = if (!privateMessage) "${event.name}: " else ""
                        runBlocking {
                            connection.say(location, "$prefix$it")
                        }
                    }
            )

            this.eventHandlers.forEach {
                try {
                    it(kotBotEvent)
                } catch (e: Exception) {
                    logger.error(e)
                }
            }
        }
    }

    /**
     * Event handlers that process KotBot events.
     */
    private val eventHandlers = mutableListOf<(KotBotEvent) -> Unit>(
            {
                if (it.command == "py") {
                    connection.say(it.source, "${it.name}: " + shellContext.eval("python", it.body))
                } else if (it.command == "js") {
                    connection.say(it.source, "${it.name}: " + shellContext.eval("js", it.body))
                }
            },
            {
                if (it.command == "help") {
                    if (it.body?.isBlank() == false) {
                        val help = helpInfo[it.body] ?: "I don't know ${it.body}"
                        it.respond(help)
                    } else {
                        it.respond("I know how to do: " + helpInfo.keys.joinToString(" "))
                    }
                }
            }
    )

    init {
        Plugins(eventHandlerAdder = ::addEventHandler, sayer = connection::say, helpAdder = ::addHelp)
    }

    private fun addHelp(command: String, help: String) {
        helpInfo[command] = help
    }

    private fun addEventHandler(handler: (KotBotEvent) -> Unit) {
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