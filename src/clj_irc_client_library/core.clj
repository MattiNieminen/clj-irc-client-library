(ns clj-irc-client-library.core
  (:use clj-irc-client-library.channel
        clj-irc-client-library.utils)
  (:import (java.net Socket)
           (java.io PrintWriter InputStreamReader BufferedReader)))

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
             (get-names-from-names-reply new-message)))
         (re-find #"PRIVMSG" reply-code)
         (merge-channel-to-connection
           connection (get-channel-name-from-privmsg new-message)
           (add-message-to-channel
             (get-channel-from-connection
               @connection (get-channel-name-from-privmsg new-message))
             (create-message (get-name-from-privmsg new-message)
                             (get-message-from-privmsg new-message))))))))

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

