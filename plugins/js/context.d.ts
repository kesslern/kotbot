declare namespace KotBotEvent {
    let message: String
    let source: String
    let command: String
    let body: String
    let name: String
    function say(message: String)
    function respond(message: String)
}

declare namespace context {
    function addEventHandler(handler: (KotBotEvent) => void)
    function say(location: String, message: String)
    function request(url: String): String
    function configString(key: String): String
    function configInt(key: String): Number
    function getValue(key: String): String
    function setValue(key: String, value: String)
    function addHelp(command: String, help: String)
}
