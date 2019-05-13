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

    init {
        val pluginContext = PluginContext(eventHandlerAdder = ::addKotBotEventHandler, responder = connection::write)
        Plugins.run(pluginContext)
    }

    private fun addKotBotEventHandler(handler: suspend (KotBotEvent) -> Unit) {
        kotbotEventHandlers.add(handler)
    }

    companion object {
        suspend fun create() {
            val connection = IrcConnection.create()
            val kotBot = KotBot(connection)
            connection.addEventHandler {
                if (it.command == "PRIVMSG") {
                    val channel = it.parameters[0]
                    val text = it.parameters[1]
                    val message = KotBotEvent(channel, text)
                    kotBot.kotbotEventHandlers.forEach { try { it(message) } catch (e: Exception) { logger.error(e) } }
                }
            }
            connection.run()
        }
    }
}