;;
;; Instrumenting HTTP with The Grinder
;;
(ns grinder-clj.example
  (:import [net.grinder.script Grinder Test]))

(let [grinder Grinder/grinder
      test (Test. 1 "Plain instrumented")]

  (defn log [& text]
    (.. grinder (getLogger) (info (apply str text))))

  ;; Function that we can record
  (defn instrumented-fn [url]
    (log "instrumented-fn: entered"))

  ;; Record calls to the instrumented function
  (.. test (record instrumented-fn))

  ;; Script returns a factory function ...
  (fn []
    ;; ... that itself returns a test function:
    (fn []
      (instrumented-fn "http://localhost:6373/version"))))
