# ロボット

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

```clojure
;; a simple echo bot
(require '[com.omarpolo.robotto/core :as robotto])

(defn start-command [{message :data :as ctx}]
  (robotto/reply-message ctx message "yo!"))

(defn on-text [{message :data :as ctx}]
  (robotto/reply-message ctx message (:text message)))

(defn run-bot []
  (let [ctx (-> (robotto/new-ctx)
                (robotto/set-token "...")
                (robotto/on-command :start start-command)
                (robotto/on-text on-text)
                (robotto/build-ctx))]
    (loop [ctx ctx]
      (recur (robotto/get-updates ctx)))))
```

## wish list

Things that are still missing

 - `:post` and `:error` interceptors?
 - add re-frame like effects
 - support more telegram api

## License

Copyright © 2020 Omar Polo <op@omarpolo.com>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
