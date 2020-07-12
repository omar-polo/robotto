# robotto examples

Here you'll find some example bot.  Every file is a *complete* bot.
To try out them, please define the `TELEGRAM_TOKEN` environment
variable with a token of a bot you control, then load the code in a
repl and execute the `(-main)` function.


### `echobot.clj`

Is a simple bot that echoes back what you write to it.  It serves as
example on how to handle commands and text, as well as buttons.


### `uppercasebot.clj`

Is a bot to show how to handle inline queries.  It will uppercase what
you write, so you can scream without holding shift ;)
