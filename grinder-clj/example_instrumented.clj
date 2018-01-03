;;
;; Instrumenting HTTP with The Grinder
;;
(ns grinder-clj.example
  (:import [net.grinder.script Grinder Test]
           [net.grinder.plugin.http HTTPRequest]))

(let [grinder Grinder/grinder
      test (Test. 1 "HTTP Instrumented")]

  (defn log [& text]
    (.. grinder (getLogger) (info (apply str text))))

  ;; Function that we can record
  (defn instrumented-get [url]
    (.. (HTTPRequest.) (GET url)))

  ;; Record calls to the instrumented function
  (.. test (record instrumented-get))

  ;; Script returns a factroy function ...
  (fn []
    ;; ... that itself returns a test function:
    (fn []
      (instrumented-get "http://localhost:6373/version"))))
