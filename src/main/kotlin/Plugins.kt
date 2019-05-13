package us.kesslern.kotbot

import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.cio.CIO
import io.ktor.client.response.readText
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import org.graalvm.polyglot.Context
import java.io.File

data class Plugin(
        val name: String,
        val language: String,
        val body: String
)

@KtorExperimentalAPI
object Plugins {
    private val pluginDir: File = File("plugins")

    private val plugins = mutableListOf<Plugin>()

    init {
        if (!pluginDir.isDirectory) {
            logger.error("Expected plugins directory to exist")
            System.exit(1)
        }
        load("js")
        load("python")
    }

    fun run(context: PluginContext) {
        val polyglotContext = Context.newBuilder().allowAllAccess(true).build()
        polyglotContext.polyglotBindings.putMember("context", context)
        plugins.forEach {
            polyglotContext.eval(it.language, it.body)
        }
    }

    private fun load(language: String) {
        val dir = pluginDir.openRelative(language)
        if (!dir.isDirectory) {
            logger.warn("Unable to locate $language plugins folder.")
            logger.warn("Not loading $language plugins.")
            return
        }
        dir.listFiles().forEach {
            logger.info("Loading $language plugin: ${it.name}")
            plugins.add(
                    Plugin(
                            name = it.name,
                            language = language,
                            body = it.readText(Charsets.UTF_8)
                    )
            )
        }
    }
}

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

fun File.openRelative(file: String) = File(this.toPath().resolve(file).toUri())
