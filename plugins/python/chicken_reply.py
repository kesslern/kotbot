responses = {'what' : 'chicken butt',
             'why'  : 'chicken thigh',
             'wat'  : 'chicken bat',
             'where': 'chicken hair',
             'when' : 'chicken pen',
             'who'  : 'chicken poo',
             'wut'  : 'chicken slut',
             'wot'  : 'chicken thot',
             'whose': 'chicken booze',
             'which': 'chicken bitch',
             'how'  : 'chicken plow'}

def chicken_reply(event):
    if event.message.lower() in responses.keys():
        message = responses[event.message.lower()]
        message = message.upper() if event.message.isupper() else message
        event.respond(message)

context.addEventHandler(chicken_reply)
