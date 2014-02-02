(ns clj-irc-client-library.core
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
    (let [new-message (.readLine (:in @connection))]
      (println new-message)
      (cond
         (re-find #"^ERROR :Closing Link:" new-message)
         (dosync (alter connection merge {:exit true}))
         (re-find #"^PING" new-message)
         (write connection (str "PONG " (re-find #":.*" new-message)))))))

(defn connect
  "Connects to IRC server and returns the connection."
  [hostname port]
  (let [socket (Socket. hostname port)
        connection (ref {:in (BufferedReader. (InputStreamReader.
                                                (.getInputStream socket)))
                         :out (PrintWriter. (.getOutputStream socket))
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
  "Joins to channel."
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
