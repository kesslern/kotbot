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
    val hostname = "irc.freenode.net"

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

        val response = input.readUTF8Line() ?: throw java.lang.RuntimeException("Could not retrieve data from server")
        Parser(response).parse()
        logger.info("Server said: '$response'")
        if (response.startsWith("PING")) {
            output.write(response.replace("PING", "PONG") + "\r\n", Charsets.US_ASCII)
        }
    }
}

class Scanner(
        private val message: String
) {
    private var position = 0

    init {
        logger.info("Scanning message: $message")
    }

    fun advance(): Char = message[position++]

    fun consume(char: Char) {
        if (message[position] == char) position++ else throw RuntimeException("Expected $char")
    }

    fun current(): Char = message[position]

    fun isCurrent(vararg chars: Char) = chars.contains(current())

    fun isCurrentLetter() = current() in 'A'..'Z' || current() in 'a'..'z'

    fun isCurrentDigit() = current() in '0'..'9'

    fun isPastEnd(): Boolean = position >= message.length

    fun parseUntil(char: Char): String {
        val end = message.findNextAfter(position, ' ') ?: throw RuntimeException("Expected $char")
        val result = message.slice(position until end)
        position = end
        return result
    }

    fun consumeWhile(condition: () -> Boolean): String {
        var result = ""
        while (!isPastEnd() && condition()) {
            result += advance()
        }
        return result
    }
}

class Parser private constructor() {
    private lateinit var scanner: Scanner

    constructor(message: String) : this() {
        scanner = Scanner(message)
    }

    fun parse() {
        parseMessage()
    }

    private fun parseMessage() {
        var prefix = ""
        val command: String?

        // Parse the prefix
        if (scanner.isCurrent(':')) {
            scanner.consume(':')
            prefix = parsePrefix()
            scanner.consume(' ')
        }

        command = parseCommand()

        while (!scanner.isPastEnd()) {
            logger.info("Found param: " + parseParam())
        }

        logger.info("'$prefix' '$command'")
    }

    private fun parseParam(): String {
        scanner.consume(' ')

        return when {
            isNotSpCrLfCl() ->
                scanner.advance() + scanner.consumeWhile {
                    isNotSpCrLfCl() || scanner.isCurrent(':')
                }
            scanner.current() == ':' ->
                scanner.advance() + scanner.consumeWhile {
                    isNotSpCrLfCl() || scanner.isCurrent(':', ' ')
                }
            else -> throw RuntimeException("Invalid parameter")
        }
    }

    private fun isNotSpCrLfCl(): Boolean = !scanner.isCurrent('\r', '\n', ' ', ':', '\u0000')

    private fun parsePrefix() = scanner.parseUntil(' ')

    private fun parseCommand(): String {
        return when {
            scanner.isCurrentLetter() ->
                scanner.consumeWhile { scanner.isCurrentLetter() }
            scanner.isCurrentDigit() ->
                scanner.consumeWhile { scanner.isCurrentDigit() }
            else -> throw RuntimeException("No command")
        }
    }
}

fun String.findNextAfter(start: Int, toFind: Char): Int? {
    for (i in start until this.length) {
        if (this[i] == toFind) return i
    }
    return null
}
