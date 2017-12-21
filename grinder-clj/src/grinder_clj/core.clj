(ns grinder-clj.core
  (:import [net.grinder Console Grinder]))

(defn -main
  "Let the leiningen manage the class path issues ..."
  [cmd & args]
  (println "grinder-clj.core/-main: entered")
  ;;
  ;; Java class main()  takes a string array which is  not the same as
  ;; Clojure vector  of strings. You  dont need to supply  anything to
  ;; Console/main, actually [1]:
  ;;
  ;; [1] http://grinder.sourceforge.net/g3/getting-started.html
  ;;
  (let [string-array (into-array String args)]
    (case cmd
      "Console" (net.grinder.Console/main string-array)
      "Grinder" (net.grinder.Grinder/main string-array)
      (println "Valid commands: Console, Grinder"))))
