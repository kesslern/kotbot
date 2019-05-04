package us.kesslern.kotbot

data class ServerMessage(
        val prefix: String?,
        val command: String,
        val parameters: List<String>
)

class MessageParser private constructor() {
    private lateinit var scanner: Scanner

    constructor(message: String) : this() {
        scanner = Scanner(message)
    }

    fun parse(): ServerMessage = parseMessage()

    private fun parseMessage(): ServerMessage {
        var prefix: String? = null
        val command: String?

        // Parse the prefix
        if (scanner.isCurrent(':')) {
            scanner.consume(':')
            prefix = parsePrefix()
            scanner.consume(' ')
        }

        command = parseCommand()

        val params = mutableListOf<String>()
        while (!scanner.isPastEnd()) {
            params += parseParam()
        }

        return ServerMessage(
                prefix = prefix,
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
