const apikey = context.configString('lastfm_api')

context.addEventHandler(event => {
    if (event.command === 'lta') {
        const response = JSON.parse(context.request(`http://ws.audioscrobbler.com/2.0/?method=user.gettopartists&user=${event.body}&api_key=${apikey}&format=json`))
        event.respond(`Top artist: ${response.topartists.artist[0].name}`)
    }
})