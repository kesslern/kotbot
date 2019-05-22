/**
 * See class KotBotEvent in KotBot.kt for full documentation for KotBotEvent field descriptions and nullability.
 */
declare namespace KotBotEvent {
    let message: String
    let channel: String
    let command: String
    let body: String
    let name: String
    function say(message: String)
    function respond(message: String)
}

/**
 * See class PluginContext in Plugins.Kt for full documentation on methods available in the global context object.
 */
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
