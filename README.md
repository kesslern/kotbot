# kotbot
An IRC bot written in Kotlin, using raw sockets backed by Ktor. Using GraalVM, plugins can be written in Javascript, Python, or Ruby.

## Run
```shell
./gradlew run
```

## TODO
- Fix ' chars that display as ? from API responses
- Support responding to users in PMs
- Add help command
  - Plugins can add commands with a description
  - help lists commands, or help with a command gets a specific description
- Create NoSQL style database interface for plugins
- Simplify responding to the user who issued a command
- Simplify responding without a user prefix
