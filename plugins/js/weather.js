context.addHelp("weather","Provide a zip code and get the weather")

const c2f = celsius => celsius * 9/5 + 32
const k2c = kelvin => kelvin - 273.15
const k2f = kelvin => c2f(k2c(kelvin))
const formatNumber = num => `${num}`.slice(0, 4)

const timeZone = context.configString("timezone")


context.addEventHandler(event => {
  if (event.command === "weather") {
    const zip = event.body
    const key = context.configString("openweathermap")
    const weather = JSON.parse(context.request("http://api.openweathermap.org/data/2.5/weather?zip=" + zip + "&APPID=" + key))

    const temp = {
        currentF: formatNumber(k2f(weather.main.temp)),
        maxF: formatNumber(k2f(weather.main.temp_max)),
        minF: formatNumber(k2f(weather.main.temp_min)),
        currentC: formatNumber(k2c(weather.main.temp)),
        maxC: formatNumber(k2c(weather.main.temp_max)),
        minC: formatNumber(k2c(weather.main.temp_min)),
    }

    const conditions = weather.weather[0].description
    const humidity = weather.main.humidity
    const wind = weather.wind.speed
    const sunrise = new Date(weather.sys.sunrise * 1000).toLocaleTimeString('en-US', {timeZone, hour: 'numeric', minute: 'numeric', hour12: true})
    const sunset =  new Date(weather.sys.sunset * 1000).toLocaleTimeString('en-US', {timeZone, hour: 'numeric', minute: 'numeric', hour12: true})

    const currentWeather = [
        `Current conditions in ${weather.name}: ${conditions}`,
        `Current temperature: ${temp.currentF}F (${temp.currentC}C)`,
        `Max temp: ${temp.maxF}F (${temp.maxC}C)`,
        `Overnight min temp: ${temp.minF}F (${temp.minC}C)`,
        `Humidity: ${humidity}/100`,
        `Wind: ${wind}m/s`,
        `Sunrise: ${sunrise}`,
        `Sunset: ${sunset}`,
    ]

    event.respond(currentWeather.join(" | "))
  }
})
