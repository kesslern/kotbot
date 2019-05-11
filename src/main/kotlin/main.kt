package us.kesslern.kotbot

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.graalvm.polyglot.Context
import java.io.File
import java.io.FileNotFoundException

val logger = KotlinLogging.logger {}

var registered = false

@KtorExperimentalAPI
fun main() = runBlocking {
    Plugins
    KotBot.create()
}

@KtorExperimentalAPI
object Plugins {
    private val pluginDir: File = File("plugins")

    private val jsPlugins = mutableMapOf<String, String>()
    private val pythonPlugins = mutableMapOf<String, String>()

    init {
        if (!pluginDir.isDirectory) {
            logger.error("Expected plugins directory to exist")
            System.exit(1)
        }
        loadJs()
        loadPython()
    }

    fun run(context: PluginContext) {
        val polyglotContext = Context.newBuilder().allowAllAccess(true).build()
        polyglotContext.polyglotBindings.putMember("context", context)
        jsPlugins.forEach { (_, script) ->
            polyglotContext.eval("js", script)
        }
        pythonPlugins.forEach { (_, script) ->
            polyglotContext.eval("python", script)
        }
    }

    private fun loadJs() {
        val jsDir = pluginDir.openRelative("js")
        if (!jsDir.isDirectory) {
            logger.warn("Unable to locate javascript plugins folder.")
            logger.warn("Not loading python plugins.")
            return
        }
        jsDir.listFiles().forEach {
            logger.info("Loading js plugin: ${it.name}")
            jsPlugins[it.name] = it.readText(Charsets.UTF_8)
        }
    }

    private fun loadPython() {
        val pythonDir = pluginDir.openRelative("python")
        if (!pythonDir.isDirectory) {
            logger.warn("Unable to locate python plugins folder.")
            logger.warn("Not loading python plugins.")
            return
        }
        pythonDir.listFiles().forEach {
            logger.info("Loading python plugin: ${it.name}")
            pythonPlugins[it.name] = it.readText(Charsets.UTF_8)
        }
    }
}

fun File.openRelative(file: String) = File(this.toPath().resolve(file).toUri())

object IrcConfig {
    val username = ConfigurationFile.requiredStringValue("username")
    val hostname = ConfigurationFile.requiredStringValue("hostname")
    val identifyOwner = ConfigurationFile.stringValue("identify_owner")
    val identifyPassword = ConfigurationFile.stringValue("identify_password")
    val channel = ConfigurationFile.requiredStringValue("channel")
    val port = ConfigurationFile.requiredIntValue("port")
}

object ConfigurationFile {
    private val json: JsonObject

    init {
        val parser: Parser = Parser.default()
        try {
            json = parser.parse("./kotbot.config.json") as JsonObject
        } catch (e: FileNotFoundException) {
            throw RuntimeException("Cannot find configuration file kotbot.config.json")
        }
    }

    fun stringValue(key: String): String? {
        val value = json.string(key)
        return if (value?.isBlank() == true) null else value
    }

    fun requiredStringValue(key: String): String {
        return stringValue(key) ?: throw RuntimeException("Missing required config for $key")
    }

    fun intValue(key: String): Int? = json.int(key)

    fun requiredIntValue(key: String): Int = intValue(key) ?: throw RuntimeException("Missing required config for $key")
}
