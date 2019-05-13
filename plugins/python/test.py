def hello(message, _):
   if message.message == "python":
       context.respond("hi from python")

context.addEventHandler(hello)