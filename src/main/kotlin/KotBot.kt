package us.kesslern.kotbot

import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.cio.CIO
import io.ktor.client.response.readText
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.error
import kotlinx.coroutines.runBlocking
import org.graalvm.polyglot.Context

data class KotbotMessage (
    @JvmField val source: String,
    @JvmField val text: String
)

@KtorExperimentalAPI
class KotBot private constructor(
        private val connection: IrcConnection
) {
    private val shellContext: Context = Context.newBuilder().build()
    private val kotbotEventHandlers = mutableListOf<suspend (KotbotMessage) -> Unit>(
            {
              if (it.text.startsWith("py ")) {
                  connection.write("PRIVMSG ${IrcConfig.channel} :" + shellContext.eval("python", it.text.substring(3)))
              } else if (it.text.startsWith("js ")) {
                  connection.write("PRIVMSG ${IrcConfig.channel} :" + shellContext.eval("js", it.text.substring(3)))
              }
            }
    )

    private val ircEventHandlers = listOf<suspend (ServerMessage) -> Unit>(
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
                    val message = KotbotMessage(channel, text)
                    kotbotEventHandlers.forEach { try { it(message) } catch (e: Exception) { logger.error(e) } }
                }
            }
    )

    private fun addKotbotEventHandler(handler: suspend (KotbotMessage) -> Unit) {
        kotbotEventHandlers.add(handler)
    }

    companion object {
        suspend fun create() {
            val connection = IrcConnection.connect(
                    hostname = IrcConfig.hostname,
                    port = IrcConfig.port
            )
            KotBot(connection).run()
        }
    }

    init {
        val pluginContext = PluginContext(eventHandlerAdder = ::addKotbotEventHandler, responder = connection::write)
        Plugins.run(pluginContext)
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
        ircEventHandlers.forEach { it(event) }
    }
}