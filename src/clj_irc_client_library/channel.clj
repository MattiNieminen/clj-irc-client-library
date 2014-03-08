(ns clj-irc-client-library.channel)

(defn create-channel
  "Creates a new channel."
  []
  {:names #{}
   :messages []})

(defn create-message
  "Creates a new message."
  [name message]
  {:name name
   :message message})

(defn merge-channel-to-connection
  "Merges new or existing channel to a connection"
  [connection name channel]
  (dosync (alter connection #(merge-with merge %1 %2)
                 {:channels {(keyword name) channel}})))

(defn add-names-to-channel
  "Adds list of names /nicks to a channel"
  [channel names]
  (let [new-names (set (clojure.set/union (:names channel) names))]
  (merge channel {:names new-names})))

(defn add-message-to-channel
  "Adds message to a channel"
  [channel message]
  (let [new-messages (conj (:messages channel) message)]
  (merge channel {:messages new-messages})))

(defn get-channel-from-connection
  "Gets channel from connection"
  [connection channel]
  (get (:channels connection) (keyword channel)))

(defn get-messages-from-channel
  "Gets channel from a connection"
  [channel]
  (:messages channel))

(defn get-messages-from-connection
  "Gets messages from a channel in connection"
  [connection channel]
  (get-messages-from-channel (get-channel-from-connection connection channel)))