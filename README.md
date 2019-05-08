# kotbot
An IRC bot written in Kotlin, using raw sockets backed by Ktor. Using GraalVM, plugins can be written in Javascript, Python, or Ruby.

## Run
```shell
./gradlew run
```

## TODO
- Parse usernames from server messages
- Parse commands based on a configurable prefix
- Figure out how to make, configure, and run a distributable jar
- Allow plugins to make HTTP requests
- Read plugins from directories
- Implement a basic weather plugin
- Create NoSQL style database interface for plugins
- Add JS and python shell with sandboxed environment
 