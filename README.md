# kotbot
An IRC bot written in Kotlin, using raw sockets backed by Ktor. Using GraalVM, plugins can be written in Javascript, Python, or Ruby.

## Run
```shell
./gradlew run
```

## TODO 
- Create NoSQL style database interface for plugins
- Add configurable prefix command
  - Parse command from prefix and add it to event passed to plugins
  - Pre-parse the rest of the text after the space
- Simplify responding to the user who issued the command
- Simplify responding in general to the whole channel
