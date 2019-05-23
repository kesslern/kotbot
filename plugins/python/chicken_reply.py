import re

pattern = re.compile("[!?]*$")

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
    # Strip off ! and ?
    stripped_message = pattern.sub('', event.message)

    if stripped_message.lower() in responses.keys():
        message = responses[stripped_message.lower()]
        message = message.upper() if stripped_message.isupper() else message
        # Add ! and ? back
        message += pattern.search(event.message).group()

        event.respond(message)

context.addEventHandler(chicken_reply)
