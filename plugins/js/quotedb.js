const retrievedQuotes  = context.getValue("quotes")

let quotes
if (retrievedQuotes) {
    quotes = JSON.parse(retrievedQuotes)
} else {
    quotes = []
}

function addQuote(event) {
    if (event.command === "addquote") {
        quotes.push(event.body)
        context.setValue("quotes", JSON.stringify(quotes))
        context.respond(`Quote #${quotes.length} added.`)
    }
}

function getQuote(event) {
    if (event.command === "quote") {

        if (quotes.length === 0) {
            event.respond("I don't know any quotes.")
            return
        }

        const sayQuote = (number) => {
            const quote = quotes[number-1]
            event.say(`Quote ${number} of ${quotes.length}: ${quote}`)
        }

        if (event.body) {
            const number = parseInt(event.body)
            if (!number) {
                event.say("What number is that?")
                return
            }
            if (number < 1 || number > quotes.length) {
                event.say("I don't know that quote.")
                return
            }
            sayQuote(number)
        } else {
            const number = Math.floor(Math.random() * quotes.length) + 1
            sayQuote(number)
        }
    }
}

context.addHelp("addquote", "Store a quote in the quote database")
context.addHelp("quote", "Get a random quote from the quote database")

context.addEventHandler(addQuote)
context.addEventHandler(getQuote)

