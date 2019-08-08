# kotbot
An IRC bot written in Kotlin, using raw sockets backed by Ktor. Using GraalVM, plugins can be written in Javascript, Python, or Ruby.

## Project Deprecation
Kotbot proved to be an interesting project and a successful POC, but development has been halted. Running each plugin in a separate context proved unscalable due to memory usage, and plugins were unable to use external libraries, restricted to only the language's standard library. This goes against the main design goal of making plugin development as simple as possible.

The project successfully demonstrates how Kotlin can dynamically load JavaScript and Python files with full language interop. Each plugin is dynamically loaded on startup and runs in its own context without access to the host filesystem or other plugins. The plugins have the ability to respond to messages, make HTTP requests, and send messages to a specific channel or user.

Backing it all is a flakey raw socket IRC implementation using Ktor's raw socket abstractions.

## Running
Copy `src/kotbot.config.hjson.example` to `src/kotbot.config.hjson` and configure.

Run with `./gradlew run`
