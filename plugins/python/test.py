def hello(message, _):
   if message.message == "python":
       message.respond("hi from python")

context.addEventHandler(hello)