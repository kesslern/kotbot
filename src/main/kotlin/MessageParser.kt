package us.kesslern.kotbot

data class ServerMessage(
        @JvmField var name: String?,
        @JvmField var user: String?,
        @JvmField var host: String?,
        @JvmField val command: String,
        @JvmField val parameters: List<String>
)

class MessageParser private constructor() {
    private lateinit var scanner: Scanner

    constructor(message: String) : this() {
        scanner = Scanner(message)
    }

    fun parse(): ServerMessage = parseMessage()

    private fun parseMessage(): ServerMessage {
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

        return ServerMessage(
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

    private fun parsePrefix() = scanner.consumeWhile { scanner.current() != ' ' }

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
