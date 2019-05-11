import polyglot
context = polyglot.import_value('context')

def hello(message, _):
   if message.text == "python":
       context.respond("hi from python")

context.addEventHandler(hello)