context.addHelp("weather","Provide a zip code and get the weather")
context.addHelp("remember", "Remember a key/value pair to be retrieved by recall")
context.addHelp("recall", "Recall a key/value pair that was stored by remember")

context.addEventHandler((event) => {
    if (event.message === "javascript") {
        event.respond("hi from javascript")
    }
    if (event.command === "weather") {
        const zip = event.body
        const key = context.configString("openweathermap")
        const weather = JSON.parse(context.request("http://api.openweathermap.org/data/2.5/weather?zip=" + zip + "&APPID=" + key))
        const temp = ("" + ((weather.main.temp - 273.15) * 9 / 5 + 32)).slice(0, 4)
        event.respond(`${weather.name} is ${weather.weather[0].main}, ${temp} degrees`)
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