(ns grinder-clj.core
  (:import [net.grinder.Console]))

(defn -main
  "I don't do a whole lot."
  [& args]
  (println "Hello, World!")
  ;; Java class main takes a string array which is not the same as
  ;; Clojure vector of strings. You dont need to supply anything to
  ;; Console/main, actually:
  (let [string-array (into-array String args)]
    (net.grinder.Console/main string-array)))
