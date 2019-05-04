package us.kesslern.kotbot

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.cio.write
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.io.readUTF8Line
import kotlinx.coroutines.runBlocking
import kotlinx.io.core.ExperimentalIoApi
import mu.KotlinLogging
import java.net.InetSocketAddress
import java.util.concurrent.Executors

val logger = KotlinLogging.logger {}

@ExperimentalIoApi
@KtorExperimentalAPI
fun main() = runBlocking {
    val port = 6667
    val hostname = "localhost"

    val exec = Executors.newCachedThreadPool()
    val selector = ActorSelectorManager(exec.asCoroutineDispatcher())

    val socket = aSocket(selector).tcp().connect(InetSocketAddress(hostname, port))
    val input = socket.openReadChannel()
    val output = socket.openWriteChannel(autoFlush = true)

    val firstResponse = input.readUTF8Line()
    logger.info("Server said: '$firstResponse'")

    output.write("NICK kotbot\r\n")
    output.write("USER kotbot 0 * :kotbot\r\n")
    output.write("JOIN #discodevs\r\n")
    output.write("PRIVMSG #discodevs :Hello from raw sockets in kotlin\r\n")

    while (!input.isClosedForRead) {
        logger.info("Reading...")

        val response = input.readUTF8Line() ?: throw RuntimeException("Could not retrieve data from server")
        logger.info("Server said: '${MessageParser(response).parse()}'")
        if (response.startsWith("PING")) {
            output.write(response.replace("PING", "PONG") + "\r\n", Charsets.US_ASCII)
        }
    }
}
