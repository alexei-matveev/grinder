(defproject grinder-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [net.sf.grinder/grinder "3.12-SNAPSHOT"
                  :exclusions [org.clojure/tools.macro]]
                 [org.clojure/tools.macro "0.1.5"]
                 ;; For test scripts:
                 [clj-http "3.7.0"]]
  :main grinder-clj.core
  ;; Implicit AOT of main is deprecated:
  :aot [grinder-clj.core]
  ;; Make Uberjar re-usable for  intrumentation. With Uberjar there by
  ;; definition just one JAR, wheather you like it or not:
  :manifest {"Premain-Class" "net.grinder.util.weave.agent.ExposeInstrumentation"
             "Can-Redefine-Classes" "true"
             "Can-Retransform-Classes" "true"})
