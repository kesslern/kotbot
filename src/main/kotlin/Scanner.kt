package us.kesslern.kotbot

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * A scanner to provide helpful parsing methods over a string while maintaining the current position in the string.
 */
class Scanner(
        private val message: String
) {
    private var position = 0

    init {
        logger.debug { "Beginning scan of message: $message" }
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

    fun consumeWhile(condition: () -> Boolean): String {
        var result = ""
        while (!isPastEnd() && condition()) {
            result += advance()
        }
        return result
    }
}
