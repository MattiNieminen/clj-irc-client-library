(ns clj-irc-client-library.channel)

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

(defn get-channel-from-connection
  "Gets channel from connection"
  [connection channel]
  (get (:channels connection) (keyword channel)))
