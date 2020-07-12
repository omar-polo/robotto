# ロボット

[![Clojars Project](https://img.shields.io/clojars/v/com.omarpolo/robotto.svg)](https://clojars.org/com.omarpolo/robotto)

`robotto` 「ロボット」 is a clojure library designed to build telegram
bots.  The idea is to embrace the interceptors and build (possibly)
complex telegram bot with them.

Instead of a framework, by using `robotto` you'll get a thin layer of
*clojureness* over the raw HTTP API, so that you don't have to
re-learn anything, but with some additional (optional) functionalities
added to ease the development.

**NB** this software is **alpha** quality: even if it's currently used
by its author in production, it lacks TONS of functionalities.  The
overall design is here to stay, but the interface are subject to
change (maybe).

## Usage

See the example folder, each file is a self-contained bot.

## wish list

Things that are still missing

 - async interceptors
 - support more telegram api
 - default interceptor per-stack with the ::* key?
 - use clojure.spec
 - use something like nippy to serialize data in callback query?

## License

Copyright © 2020 Omar Polo <op@omarpolo.com>

Distributed under the ISC License.
