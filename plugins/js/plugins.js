context.addHelp("catfact", "Get a fun fact about cats.")
context.addHelp("insult", "Get a random insult.")
context.addHelp("trump", "Get a random Trump quote.")
context.addHelp("kanye", "Get a random Kanye quote.")
context.addHelp("advice", "Get random advice.")

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
  if (event.command === "urbandictionary" || event.command === "ud") {
    const param = encodeURIComponent(event.body)
    const {
      definition
    } = JSON.parse(context.request(`http://api.urbandictionary.com/v0/define?term=${param}`)).list[0]
    event.respond(definition.replace('[', '').replace(']', ''))
  }
})
