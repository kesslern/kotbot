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
        Parser(response).parse()
        logger.info("Server said: '$response'")
        if (response.startsWith("PING")) {
            output.write(response.replace("PING", "PONG") + "\r\n", Charsets.US_ASCII)
        }
    }
}

class Scanner(
        val message: String
) {
    var position = 0

    init {
        logger.info("Scanning message: $message")
    }

    fun advance(): Char {
        position++
        return message[position - 1]
    }

    fun consume(char: Char): Unit {
        if (message[position] == char) position++ else throw RuntimeException("Expected $char")
    }

    fun current(): Char = message[position]

    fun isAtEnd(): Boolean = position == message.length - 1

    fun parseUntil(char: Char): String {
        val end = message.findNextAfter(position, ' ') ?: throw RuntimeException("Expected $char")
        val result = message.slice(position until end)
        position = end
        return result
    }
}

class Parser(

) {
    private lateinit var scanner: Scanner

    constructor(message: String) : this() {
        scanner = Scanner(message)
    }

    fun parse() {
        parseMessage()
    }

    fun parseMessage() {
        var prefix = ""
        val command: String?

        // Parse the prefix
        if (scanner.current() == ':') {
            scanner.advance() // consume ':'
            prefix = parsePrefix()
            scanner.advance() // consume space
        }

        command = parseCommand()

        while (!scanner.isAtEnd()) {
            logger.info("Found param: " + parseParam())
        }
        // parse params while not crlf next

        logger.info("'$prefix' '$command'")
    }

    private fun parseParam(): String {
        scanner.consume(' ')

        var middle = ""
        if (isNoSpCrLfCl()) {
            middle += scanner.advance()
            while ((isNoSpCrLfCl() || scanner.current() == ':') && !scanner.isAtEnd()) {
                middle += scanner.advance()
            }
        } else if (scanner.current() == ':') {
            scanner.advance()
            while ((isNoSpCrLfCl() || scanner.current() == ':' || scanner.current() == ' ') && !scanner.isAtEnd()) {
                middle += scanner.advance()
            }
        }
        return middle
    }

    private fun isNoSpCrLfCl(): Boolean =
            !listOf('\r', '\n', ' ', ':', '\u0000').contains(scanner.current())

    private fun parsePrefix() = scanner.parseUntil(' ')

    private fun parseCommand(): String {
        var command = ""
        if (scanner.current() in 'A'..'Z' || scanner.current() in 'a'..'z') {
            while (scanner.current() in 'A'..'Z' || scanner.current() in 'a'..'z') {
                command += scanner.advance()
            }
        } else if (scanner.current() in '0'..'9') {
            while (scanner.current() in '0'..'9') {
                command += scanner.advance()
            }
        } else {
            throw RuntimeException("No command")
        }
        return command
    }
}

fun String.findNextAfter(start: Int, toFind: Char): Int? {
    for (i in start until this.length) {
        if (this[i] == toFind) return i
    }
    return null
}
