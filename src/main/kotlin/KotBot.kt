package us.kesslern.kotbot

import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.cio.CIO
import io.ktor.client.response.readText
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.error
import kotlinx.coroutines.runBlocking
import org.graalvm.polyglot.Context

@KtorExperimentalAPI
class PluginContext(
        val eventHandlerAdder:  (suspend (KotbotMessage) -> Unit) -> Unit,
        val responder: suspend (String) -> Unit
) {
    private val client = HttpClient(CIO)

    fun addEventHandler(handler: suspend (KotbotMessage) -> Unit) {
        eventHandlerAdder(handler)
    }

    fun respond(response: String) {
        runBlocking { responder("PRIVMSG ${IrcConfig.channel} :$response") }
    }

    fun request(url: String): String {
        return runBlocking {
            client.call(url).response.readText()
        }
    }

    fun configString(key: String): String? = ConfigurationFile.stringValue(key)

    fun configInt(key: String): Int? = ConfigurationFile.intValue(key)
}

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
                    connection.write("PRIVMSG ${IrcConfig.channel} :Hello from raw sockets in kotlin")
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
        val polyglotContext = Context.newBuilder().allowAllAccess(true).build()
        val pluginContext = PluginContext(eventHandlerAdder = ::addKotbotEventHandler, responder = connection::write)
        polyglotContext.polyglotBindings.putMember("context", pluginContext)
        polyglotContext.eval("js", """
            var context = Polyglot.import('context')
            context.addEventHandler((message) => {
            if (message.text == "javascript") {
                context.respond("hi from javascript")
            }
            if (message.text.startsWith("weather")) {
                const zip = message.text.split(" ")[1]
                const key = context.configString("openweathermap")
                const weather = JSON.parse(context.request("http://api.openweathermap.org/data/2.5/weather?zip=" + zip + "&APPID=" + key))
                const temp = ("" + ((weather.main.temp - 273.15) * 9/5 + 32)).slice(0,4)
                context.respond(`${"$"}{weather.name} is ${"$"}{weather.weather[0].main}, ${"$"}{temp} degrees`)
            }
        })
        """)

        polyglotContext.eval("python", """
import polyglot
context = polyglot.import_value('context')

def hello(message, _):
   if message.text == "python":
       context.respond("hi from python")

context.addEventHandler(hello)
        """)
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