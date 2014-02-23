(ns clj-irc-client-library.utils
  (:use [clojure.string :only [split trim]]))

(defn words
  "Splits string by whitespace"
  [words]
  (split words #"\s+"))

(defn get-channel-name-from-names-reply
  "Gets channel name from reply message"
  [message]
  (trim (subs (re-find #"[@|=] #.[^\s]*" message) 1)))

(defn get-names-from-names-reply
  "Gets names as list from reply message"
  [message]
  (words (subs (re-find #" :.*" message) 2)))