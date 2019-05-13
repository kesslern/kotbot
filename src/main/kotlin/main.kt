package us.kesslern.kotbot

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun main() = runBlocking {
    Plugins
    KotBot.create()
}
