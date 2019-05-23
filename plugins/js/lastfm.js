const apikey = context.configString('lastfm_api')

context.addEventHandler(event => {
    if (event.command === 'lta') {
        const response = JSON.parse(context.request(`http://ws.audioscrobbler.com/2.0/?method=user.gettopartists&user=${event.body}&api_key=${apikey}&format=json`))
        const artists = response.topartists.artist

        const result = []
        for (let i = 0; i < 10 && i < artists.length; i++) {
            const artist = artists[i]
            result.push({
                artist: artist.name,
                count: artist.playcount,
            })
         }

        const top = result.map(it => `${it.artist} [${it.count}]`).join(' | ')
        event.respond(`Top artists: ${top}`)
    }
})