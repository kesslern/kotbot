package us.kesslern.kotbot

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
import java.net.InetSocketAddress
import java.util.concurrent.Executors

@KtorExperimentalAPI
class IrcConnection private constructor () {
    private val exec = Executors.newCachedThreadPool()
    private val selector = ActorSelectorManager(exec.asCoroutineDispatcher())

    private lateinit var socket: Socket
    private lateinit var input: ByteReadChannel
    private lateinit var output: ByteWriteChannel

    companion object {
        suspend fun connect(
                hostname: String,
                port: Int
        ): IrcConnection {
            val connection = IrcConnection()
            connection.connect(hostname, port)
            return connection
        }
    }

    private suspend fun connect(
            hostname: String,
            port: Int
    ) {
        socket = aSocket(selector).tcp().connect(InetSocketAddress(hostname, port))
        input = socket.openReadChannel()
        output = socket.openWriteChannel(autoFlush = true)
    }

    suspend fun readLine(): String =
            input.readUTF8Line() ?: throw RuntimeException("Could not retrieve data from server")

    suspend fun write(data: String) {
        val dataWithLineEndings = if (data.endsWith("\r\n")) data else data + "\r\n"
        logger.info("Writing: ${dataWithLineEndings.trim()}")
        output.write(dataWithLineEndings, Charsets.US_ASCII)
    }

    fun isClosedForRead() = input.isClosedForRead
}