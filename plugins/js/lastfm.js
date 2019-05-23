const apikey = context.configString('lastfm_api')

const periods = {
    overall: 'overall',
    week: '7day',
    month: '1month',
}

function getTopArtists(user, period) {
    const artists = JSON.parse(
        context.request(
            `http://ws.audioscrobbler.com/2.0/?method=user.gettopartists&user=${user}&api_key=${apikey}&format=json&limit=10&period=${period}`
        )
    ).topartists.artist

    const result = []
    for (let i = 0; i < 10 && i < artists.length; i++) {
        const artist = artists[i]
        result.push({
            artist: artist.name,
            count: artist.playcount,
        })
    }
    return result
}

function respond(event, header, artists) {
    const top = artists
        .map(it => `${it.artist} [${it.count}]`)
        .join(' | ')
    event.respond(`${header}: ${top}`)
}

context.addEventHandler(event => {
    if (event.command === 'lta') {
        const artists = getTopArtists(event.body, periods.overall)
        respond(event, "Top artists, overall", artists)
    }
    if (event.command === 'ltm') {
        const artists = getTopArtists(event.body, periods.month)
        respond(event, "Top monthly artists", artists)
    }
    if (event.command === 'ltw') {
        const artists = getTopArtists(event.body, periods.week)
        respond(event, "Top weekly artists", artists)
    }
})