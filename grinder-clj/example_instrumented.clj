;;
;; Instrumenting HTTP with The Grinder
;;
(ns grinder-clj.example
  ;; Grinder uses http-kit already, that is why:
  (:require [org.httpkit.client :as http])
  (:import [net.grinder.script Grinder Test]))

(let [grinder Grinder/grinder
      test (Test. 1 "Plain instrumented")]

  (defn log [& text]
    (.. grinder (getLogger) (info (apply str text))))

  ;; Function that we can "record":
  (defn instrumented-fn [url]
    ;; Deref is needed for http-kit, no deref for clj-http. Body is an
    ;; http-kit specific InputStream.
    (let [req @(http/get url)]
      (log "body: " (slurp (:body req)))))

  ;; "Record" calls to the instrumented function
  (.. test (record instrumented-fn))

  ;; Script returns a factory function ...
  (fn []
    ;; ... that itself returns a test function:
    (fn []
      (instrumented-fn "http://localhost:6373/version"))))
