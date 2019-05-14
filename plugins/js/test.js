context.addEventHandler((event) => {
    if (event.message == "javascript") {
        context.respond("hi from javascript")
    }
    if (event.command === "weather") {
        const zip = event.body
        const key = context.configString("openweathermap")
        const weather = JSON.parse(context.request("http://api.openweathermap.org/data/2.5/weather?zip=" + zip + "&APPID=" + key))
        const temp = ("" + ((weather.main.temp - 273.15) * 9 / 5 + 32)).slice(0, 4)
        event.respond(`${weather.name} is ${weather.weather[0].main}, ${temp} degrees`)
    }
})