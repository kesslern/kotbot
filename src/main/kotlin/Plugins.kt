package us.kesslern.kotbot

import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.cio.CIO
import io.ktor.client.response.readText
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.graalvm.polyglot.Context
import java.io.File

private val logger = KotlinLogging.logger {}

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
        private val eventHandlerAdder:  ((KotBotEvent) -> Unit) -> Unit,
        private val sayer: (String, String) -> Unit,
        private val helpAdder: (String, String) -> Unit
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
            logger.debug("Starting plugin ${plugin.name}")
            val polyglotContext = Context.newBuilder().allowAllAccess(true).build()
            val pluginContext = PluginContext(
                    eventHandlerAdder,
                    sayer,
                    getDatabaseValue = { key: String ->
                        Database.getPluginData(plugin.name, key)
                    },
                    setDatabaseValue = { key: String, value: String ->
                        Database.setPluginData(plugin.name, key, value)
                    },
                    helpAdder = helpAdder
            )

            polyglotContext.polyglotBindings.putMember("context", pluginContext)
            polyglotContext.eval(plugin.language, PluginHeaders[plugin.language])
            polyglotContext.eval(plugin.language, plugin.body)
        }
    }

    private fun load(language: String, extension: String) {
        logger.debug("Loading $language plugins with extension $extension")
        val dir = pluginDir.openRelative(language)
        if (!dir.isDirectory) {
            logger.warn("Unable to locate $language plugins folder.")
            logger.warn("Not loading $language plugins.")
            return
        }
        dir.listFiles().forEach {
            if (!it.name.endsWith(extension)) {
                logger.info("Skipping non-$language file: ${it.name}")
                return@forEach
            }
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
        private val eventHandlerAdder:  ((KotBotEvent) -> Unit) -> Unit,
        private val sayer: (String, String) -> Unit,
        private val getDatabaseValue: (String) -> String?,
        private val setDatabaseValue: (String, String) -> Unit,
        private val helpAdder: (String, String) -> Unit
) {
    private val client = HttpClient(CIO)

    fun addEventHandler(handler: (KotBotEvent) -> Unit) {
        eventHandlerAdder(handler)
    }

    fun say(location: String, message: String) {
        sayer(location, message)
    }

    fun request(url: String): String {
        logger.info("Requesting URL: $url")
        return runBlocking {
            client.call(url).response.readText()
        }
    }

    fun getValue(key: String): String? {
        logger.debug("Getting database value: $key")
        return this.getDatabaseValue(key)
    }

    fun setValue(key: String, value: String) {
        logger.debug("Setting database value: $key")
        this.setDatabaseValue(key, value)
    }

    fun addHelp(command: String, help: String) {
        this.helpAdder(command, help)
    }

    fun configString(key: String): String? = ConfigurationFile.stringValue(key)

    fun configInt(key: String): Int? = ConfigurationFile.intValue(key)
}

fun File.openRelative(file: String) = File(this.toPath().resolve(file).toUri())
