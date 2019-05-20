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

val PluginHeaders = mapOf(
        "js" to """
            const context = Polyglot.import('context')
        """.trimIndent(),
        "python" to """
            import polyglot
            context = polyglot.import_value('context')
        """.trimIndent()
)

/**
 * Singleton object for loading and running plugins.
 */
@KtorExperimentalAPI
class Plugins(
        val eventHandlerAdder:  (suspend (KotBotEvent) -> Unit) -> Unit,
        val sayer: suspend (String, String) -> Unit
) {
    private val pluginDir: File = File("plugins")

    private val plugins = mutableListOf<Plugin>()

    /**
     * Load all plugins on initialization.
     */
    init {
        if (!pluginDir.isDirectory) {
            logger.error("Expected plugins directory to exist")
            System.exit(1)
        }
        load("js", ".js")
        load("python", ".py")

        plugins.forEach { plugin ->
            val polyglotContext = Context.newBuilder().allowAllAccess(true).build()
            val pluginContext = PluginContext(
                    eventHandlerAdder,
                    sayer,
                    getDatabaseValue = { key: String ->
                        Database.connection.getPluginData(plugin.name, key)
                    },
                    setDatabaseValue = { key: String, value: String ->
                        logger.info("Name: ${plugin.name} Key: $key Value: $value")
                        Database.connection.setPluginData(plugin.name, key, value)
                    }
            )

            polyglotContext.polyglotBindings.putMember("context", pluginContext)
            polyglotContext.eval(plugin.language, PluginHeaders[plugin.language])
            polyglotContext.eval(plugin.language, plugin.body)
        }
    }

    private fun load(language: String, extension: String) {
        val dir = pluginDir.openRelative(language)
        if (!dir.isDirectory) {
            logger.warn("Unable to locate $language plugins folder.")
            logger.warn("Not loading $language plugins.")
            return
        }
        dir.listFiles().forEach {
            if (!it.name.endsWith(extension)) return@forEach
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

/**
 * An instance of this class is created and passed to the environment plugins run in. Each plugin has access to this
 * context object. This class is the bridge between Kotlin code and the plugins.
 */
@KtorExperimentalAPI
class PluginContext(
        // TODO: Make this not use suspend
        val eventHandlerAdder:  (suspend (KotBotEvent) -> Unit) -> Unit,
        val sayer: suspend (String, String) -> Unit,
        val getDatabaseValue: (String) -> String?,
        val setDatabaseValue: (String, String) -> Unit
) {
    private val client = HttpClient(CIO)

    fun addEventHandler(handler: suspend (KotBotEvent) -> Unit) {
        eventHandlerAdder(handler)
    }

    fun say(location: String, message: String) {
        runBlocking {
            sayer(location, message)
        }
    }

    fun request(url: String): String {
        return runBlocking {
            client.call(url).response.readText()
        }
    }

    fun getValue(key: String): String? {
        return this.getDatabaseValue(key)
    }

    fun setValue(key: String, value: String) {
        this.setDatabaseValue(key, value)
    }

    fun configString(key: String): String? = ConfigurationFile.stringValue(key)

    fun configInt(key: String): Int? = ConfigurationFile.intValue(key)
}

fun File.openRelative(file: String) = File(this.toPath().resolve(file).toUri())
