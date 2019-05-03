
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
fun main() = runBlocking<Unit> {
    val port = 6667
    val hostname = "irc.freenode.net"

    val exec = Executors.newCachedThreadPool()
    val selector = ActorSelectorManager(exec.asCoroutineDispatcher())

    val socket = aSocket(selector).tcp().connect(InetSocketAddress(hostname, port))
    val input = socket.openReadChannel()
    val output = socket.openWriteChannel(autoFlush = true)

    val response = input.readUTF8Line()
    logger.info("Server said: '$response'")

    output.write("NICK kotbot\r\n")
    output.write("USER kotbot 0 * :kotbot\r\n")
    output.write("JOIN #discodevs\r\n")
    output.write("PRIVMSG #discodevs :Hello from raw sockets in kotlin\r\n")

    while (!input.isClosedForRead) {
        logger.info("Reading...")

        val response = input.readUTF8Line() ?: throw java.lang.RuntimeException("idk")
        parseMessage(response)
        logger.info("Server said: '$response'")
        if (response.startsWith("PING")) {
            output.write(response.replace("PING", "PONG") + "\r\n", Charsets.US_ASCII)
        }
    }
}

var position = 0

fun parseMessage(message: String) {
    logger.info("Parsing message: " + message)
    var prefix = ""
    var command: String?

    position = 0
    if (message[position] == ':') {
        position++ // consume ':'
        prefix = parsePrefix(message)
        position++ // consume space
    }
    command = parseCommand(message)
    while (position != message.length - 1 && message[position+1] != '\r') {
        logger.info("Found param: " + parseParam(message))
    }
    // parse params while not crlf next

    logger.info("'$prefix' '$command' '${message.slice(position until message.length)}'")
}

fun parseParam(message: String): String {
    if (message[position] != ' ') throw RuntimeException("No space")
    position++ // consume space

    var middle = ""
    if (isNoSpCrLfCl(message)) {
        middle += message[position]
        position++
        while ((isNoSpCrLfCl(message) || message[position] == ':') && position != message.length - 1) {
            middle += message[position]
            position++
        }
    } else if (message[position] == ':') {
        position++
        while ((isNoSpCrLfCl(message) || message[position] == ':' || message[position] == ' ') && position != message.length - 1) {
            middle += message[position]
            position++
        }
    }
    return middle
}

fun isNoSpCrLfCl(message: String): Boolean =
        !listOf('\r', '\n', ' ', ':', '\u0000').contains(message[position])

fun parsePrefix(message: String): String {
    val end = message.findNextAfter(position, ' ') ?: throw RuntimeException("missing space")
    val prefix = message.slice(position until end)
    position = end
    return prefix
}

fun parseCommand(message: String): String {
    var command = ""
    if (message[position] in 'A'..'Z' || message[position] in 'a'..'z') {
        while (message[position] in 'A'..'Z' || message[position] in 'a'..'z') {
            command += message[position++]
        }
    } else if (message[position] in '0'..'9') {
        while (message[position] in '0'..'9') {
            command += message[position++]
        }
    } else {
        throw RuntimeException("No command")
    }
    return command
}

fun String.findNextAfter(start: Int, toFind: Char): Int? {
    for (i in start until this.length) {
        if (this[i] == toFind) return i
    }
    return null
}
