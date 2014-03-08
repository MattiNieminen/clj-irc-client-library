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

(defn get-channel-name-from-privmsg
  "Gets channel name from privmsg"
  [message]
  (nth (words message) 2))

(defn get-name-from-privmsg
  "Gets name / nick from privmsg"
  [message]
  (subs (re-find #":.[^!]*" message) 1))

(defn get-message-from-privmsg
  "Gets message from privmsg"
  [message]
  (last (split message #":")))