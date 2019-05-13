package us.kesslern.kotbot

/**
 * A parsed message from the server.
 * Refer to https://tools.ietf.org/html/rfc2812#section-2.3.1 for details.
 */
data class ServerEvent(
        /** Servername or username from prefix. */
        @JvmField var name: String?,
        /** User from prefix. */
        @JvmField var user: String?,
        /** Host from prefix. */
        @JvmField var host: String?,
        /** Command from message. */
        @JvmField val command: String,
        /** List of parsed parameters. */
        @JvmField val parameters: List<String>
)

/**
 * Parser for raw messages from the IRC server.
 * Refer to https://tools.ietf.org/html/rfc2812#section-2.3.1 for details.
 */
class MessageParser private constructor() {
    private lateinit var scanner: Scanner

    private constructor(message: String) : this() {
        scanner = Scanner(message)
    }

    private fun parseMessage(): ServerEvent {
        var name: String? = null
        var user: String? = null
        var host: String? = null
        val command: String?

        // Parse the prefix
        if (scanner.isCurrent(':')) {
            scanner.consume(':')
            name = scanner.consumeWhile { !scanner.isCurrent(' ', '!', '@') }
            if (scanner.isCurrent('!')) {
                scanner.consume('!')
                user = scanner.consumeWhile { !scanner.isCurrent(' ', '@') }
            }
            if (scanner.isCurrent('@')) {
                scanner.consume('@')
                host = scanner.consumeWhile { !scanner.isCurrent(' ') }
            }
            scanner.consume(' ')
        }

        command = parseCommand()

        val params = mutableListOf<String>()
        while (!scanner.isPastEnd()) {
            params += parseParam()
        }

        return ServerEvent(
                name = name,
                user = user,
                host = host,
                command = command,
                parameters = params
        )
    }

    private fun parseParam(): String {
        scanner.consume(' ')

        return when {
            isNotSpCrLfCl() ->
                scanner.advance() + scanner.consumeWhile {
                    isNotSpCrLfCl() || scanner.isCurrent(':')
                }
            scanner.current() == ':' -> {
                scanner.advance()
                scanner.consumeWhile {
                    isNotSpCrLfCl() || scanner.isCurrent(':', ' ')
                }
            }
            else -> throw RuntimeException("Invalid parameter")
        }
    }

    private fun isNotSpCrLfCl(): Boolean = !scanner.isCurrent('\r', '\n', ' ', ':', '\u0000')

    private fun parseCommand(): String {
        return when {
            scanner.isCurrentLetter() ->
                scanner.consumeWhile { scanner.isCurrentLetter() }
            scanner.isCurrentDigit() ->
                scanner.consumeWhile { scanner.isCurrentDigit() }
            else -> throw RuntimeException("No command")
        }
    }

    companion object {
        fun parse(message: String) = MessageParser(message).parseMessage()
    }
}
