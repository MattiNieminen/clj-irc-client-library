(ns clj-irc-client-library.core
  (:import (java.net Socket)
           (java.io PrintWriter InputStreamReader BufferedReader))
  (:use [clojure.string :only [split trim]]))

(defn words
  "Splits string by whitespace"
  [words]
  (split words #"\s+"))

(defn create-channel
  "Creates a new channel."
  []
  {:names #{}
   :messages ()})

(defn merge-channel-to-connection
  "Testing"
  [connection name channel]
  (dosync (alter connection #(merge-with merge %1 %2)
                 {:channels {(keyword name) channel}})))

(defn add-names-to-channel
  "Adds list of names /nicks to a channel"
  [channel names]
  (let [new-names (set (clojure.set/union (:names channel) names))]
  (merge channel {:names new-names})))

(defn get-channel-name-from-names-reply
  "Gets channel name from reply message"
  [message]
  (trim (subs (re-find #"[@|=] #.[^\s]*" message) 1)))

(defn get-names-from-names-reply
  "Gets names as list from reply message"
  [message]
  (words (subs (re-find #" :.*" message) 2)))

(defn get-channel-from-connection
  "Gets channel from connection"
  [connection channel]
  (get (:channels connection) (keyword channel)))

(defn write
  "Writes messages to the connection output and flushes."
  [connection message]
  (doto (:out @connection)
    (.println (str message "\r"))
    (.flush)))

(defn handle-connection
  "Handles irc connection created by connect. Takes care of PING PONG and
  prints incoming messages."
  [connection]
  (while (nil? (:exit @connection))
    (let [new-message (.readLine (:in @connection))
          message-words (words new-message)
          reply-code (second message-words)]
      (println new-message)
      (cond
         (re-find #"^ERROR :Closing Link:" new-message)
         (dosync (alter connection merge {:exit true}))
         (re-find #"^PING" new-message)
         (write connection (str "PONG " (re-find #":.*" new-message)))
         (re-find #"^004" reply-code)
         (merge-channel-to-connection connection (nth message-words 3)
                                      (create-channel))
         (re-find #"JOIN" reply-code)
         (merge-channel-to-connection
           connection (re-find #"#.*" (nth message-words 2)) (create-channel))
         (re-find #"353" reply-code)
         (merge-channel-to-connection
           connection (get-channel-name-from-names-reply new-message)
           (add-names-to-channel
             (get-channel-from-connection
               @connection (get-channel-name-from-names-reply new-message))
             (get-names-from-names-reply new-message)))))))

(defn connect
  "Connects to IRC server and returns the connection."
  [hostname port]
  (let [socket (Socket. hostname port)
        connection (ref {:in (BufferedReader. (InputStreamReader.
                                                (.getInputStream socket)))
                         :out (PrintWriter. (.getOutputStream socket))
                         :channels {}
                         :exit nil})]
    (future (handle-connection connection))
    connection))

;TODO should there be just one form with do to clearly indicate side effects?
(defn login
  "Sends necessary login messages to IRC server."
  [connection nick user mode real-name]
  (write connection (str "NICK " nick))
  (write connection (str "USER " user " " mode " * :" real-name)))

(defn join
  "Joins to a channel."
  [connection channel]
  (cond
    (re-find #"^#" channel)
    (write connection (str "JOIN " channel))
    :else
    (write connection (str "JOIN #" channel))))

(defn part
  "Leaves a channel."
  [connection channel]
  (cond
    (re-find #"^#" channel)
    (write connection (str "PART " channel))
    :else
    (write connection (str "PART #" channel))))

(defn speak
  "Sends message to a user or channel (PRIVMSG)."
  [connection target message]
  (write connection (str "PRIVMSG " target " :" message)))

(defn disconnect
  "Closes the connection to server."
  [connection message]
  (write connection (str "QUIT :" message))
  (dosync (alter connection merge {:exit true})))

