package us.kesslern.kotbot

import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.error
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.graalvm.polyglot.Context

private val logger = KotlinLogging.logger {}

/**
 * An event that is passed to plugins and built-in bot event handlers. An event is passed for each message received.
 */
data class KotBotEvent(
        /** The entire message sent by the other person. */
        @JvmField val message: String,

        /** The channel the message was sent in. Null if private message. */
        @JvmField val channel: String?,

        /** If the message was sent with the bot's command prefix, the string between the prefix and the first space. */
        @JvmField val command: String?,

        /** Any text in the message that follows the command. */
        @JvmField val body: String?,

        /** The username of the sender of the message. */
        @JvmField val name: String,

        /** Say a message in the channel or private message session from which the event came. */
        @JvmField val sayer: (String) -> Unit,

        /** Respond in the channel or private message session from which the event came, highlighting the user who
         * originated the event. */
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

            val privateMessage = !source.startsWith("#")
            val location = if (privateMessage) event.name!! else source

            val kotBotEvent = KotBotEvent(
                    channel = if (source.startsWith("#")) source else null,
                    message = message,
                    command = command,
                    body = body,
                    name = event.name!!,
                    sayer = {
                        runBlocking {
                            logger.info("Saying to $location: $it")
                            connection.say(location, it)
                        }
                    },
                    responder = {
                        val prefix = if (!privateMessage) "${event.name}: " else ""
                        logger.info("Saying to $location: $prefix$it")
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
                    logger.info("Running Python command: ${it.body}")
                    it.say("${it.name}: " + shellContext.eval("python", it.body))
                } else if (it.command == "js") {
                    logger.info("Running JS command: ${it.body}")
                    it.say("${it.name}: " + shellContext.eval("js", it.body))
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
        logger.debug("Initializing plugins")
        Plugins(eventHandlerAdder = ::addEventHandler, sayer = connection::say, helpAdder = ::addHelp)
    }

    private fun addHelp(command: String, help: String) {
        logger.debug("Adding help for command $command")
        logger.trace("Help: $help")
        helpInfo[command] = help
    }

    private fun addEventHandler(handler: (KotBotEvent) -> Unit) {
        eventHandlers.add(handler)
    }

    companion object {
        suspend fun create() {
            logger.info("Starting KotBot")
            val connection = IrcConnection.create()
            val kotBot = KotBot(connection)
            connection.addEventHandler(kotBot.kotBotEventCaller)
            connection.run()
        }
    }
}