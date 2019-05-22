context.addHelp("remember", "Remember a key/value pair to be retrieved by recall")
context.addHelp("recall", "Recall a key/value pair that was stored by remember")

context.addEventHandler((event) => {
    if (event.message === "javascript") {
        event.say("hi from javascript")
    }
    if (event.command === "remember") {
        const str = event.body
        const key = str.substr(0, str.indexOf(' '))
        const value = str.substr(str.indexOf(' ')+1)
        context.setValue(key, value)
    }
    if (event.command === "recall") {
        event.respond(context.getValue(event.body))
    }
})