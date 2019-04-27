
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.cio.write
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.readUTF8Line
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.util.concurrent.Executors

@KtorExperimentalAPI
fun main(): Unit = runBlocking {
    val exec = Executors.newCachedThreadPool()
    val selector = ActorSelectorManager(exec.asCoroutineDispatcher())

    val socket = aSocket(selector).tcp().connect(InetSocketAddress("irc.freenode.net", 6667))
    KotBot(socket)
    Unit
}

@KtorExperimentalAPI
class KotBot(
        socket: Socket
) {
    private val input: ByteReadChannel = socket.openReadChannel()
    private val output: ByteWriteChannel = socket.openWriteChannel(autoFlush = true)

    init { runBlocking {
        launch {
            while (!input.isClosedForRead) {
                val response = input.readUTF8Line()
                println("Server said: '$response'")
                if (response?.startsWith("PING") == true) {
                    output.write(response.replace("PING", "PONG"), Charsets.US_ASCII)
                }
            }
        }
        launch {
            output.write("NICK kotbot\r\n", Charsets.US_ASCII)
            output.write("USER kotbot 0 * :kotbot\r\n", Charsets.US_ASCII)
            output.write("JOIN #discodevs\r\n", Charsets.US_ASCII)
            output.write("PRIVMSG #discodevs :Hello from raw sockets in kotlin\r\n", Charsets.US_ASCII)
        } }
    }
}