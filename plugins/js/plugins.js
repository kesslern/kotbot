context.addEventHandler(event => {
    if (event.command === "catfact") {
        const fact = JSON.parse(context.request("https://cat-fact.herokuapp.com/facts/random"))
        event.respond(fact.text)
    }
    if (event.command === "insult") {
        const {
            insult
        } = JSON.parse(context.request("https://evilinsult.com/generate_insult.php?lang=en&type=json"))
        event.respond(insult)
    }
    if (event.command === "trump") {
        const {
            value
        } = JSON.parse(context.request("https://api.tronalddump.io/random/quote"))
        event.respond(value)
    }
    if (event.command === "kanye") {
        const {
            quote
        } = JSON.parse(context.request("https://api.kanye.rest"))
        event.respond(quote)
    }
    if (event.command === "advice") {
        const {
            advice
        } = JSON.parse(context.request("https://api.adviceslip.com/advice")).slip
        event.respond(advice)
    }
})
