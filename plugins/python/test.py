def hello(message, _):
   if message.message == "python":
       event.respond("hi from python")

context.addEventHandler(hello)