context.addEventHandler(event => {
    if (event.command === "catfact") {
        const fact = JSON.parse(context.request("https://cat-fact.herokuapp.com/facts/random"))
        context.respond(fact.text)
    }
    if (event.command === "insult") {
        const {
            insult
        } = JSON.parse(context.request("https://evilinsult.com/generate_insult.php?lang=en&type=json"))
        context.respond(insult)
    }
    if (event.command === "trump") {
        const {
            value
        } = JSON.parse(context.request("https://api.tronalddump.io/random/quote"))
        context.respond(value)
    }
    if (event.command === "kanye") {
        const {
            quote
        } = JSON.parse(context.request("https://api.kanye.rest"))
        context.respond(quote)
    }
    if (event.command === "advice") {
        const {
            advice
        } = JSON.parse(context.request("https://api.adviceslip.com/advice")).slip
        context.respond(advice)
    }
})
