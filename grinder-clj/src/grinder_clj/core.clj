(ns grinder-clj.core
  (:require [clojure.tools.logging :as log])
  (:import [net.grinder Console Grinder])
  (:gen-class))

(defn -main
  "Let the leiningen manage the class path issues ..."
  [cmd & args]
  (println "grinder-clj.core/-main: entered")

  ;;
  ;; Exceptions in separate threads a silent by default [1], that is
  ;; why.
  ;;
  ;; [1] https://stuartsierra.com/2015/05/27/clojure-uncaught-exceptions
  ;;
  (Thread/setDefaultUncaughtExceptionHandler
   (reify Thread$UncaughtExceptionHandler
     (uncaughtException [_ thread ex]
       (log/error ex "Uncaught exception on" (.getName thread)))))

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
