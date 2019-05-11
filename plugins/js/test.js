var context = Polyglot.import('context')
context.addEventHandler((message) => {
    if (message.text == "javascript") {
        context.respond("hi from javascript")
    }
    if (message.text.startsWith("weather")) {
        const zip = message.text.split(" ")[1]
        const key = context.configString("openweathermap")
        const weather = JSON.parse(context.request("http://api.openweathermap.org/data/2.5/weather?zip=" + zip + "&APPID=" + key))
        const temp = ("" + ((weather.main.temp - 273.15) * 9 / 5 + 32)).slice(0, 4)
        context.respond(`${weather.name} is ${weather.weather[0].main}, ${temp} degrees`)
    }
})