package us.kesslern.kotbot

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

    fun consumeWhile(condition: () -> Boolean): String {
        var result = ""
        while (!isPastEnd() && condition()) {
            result += advance()
        }
        return result
    }
}
