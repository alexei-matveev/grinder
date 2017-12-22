(defproject grinder-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [net.sf.grinder/grinder "3.12-SNAPSHOT"
                  :exclusions [org.clojure/tools.macro]]
                 [org.clojure/tools.macro "0.1.5"]]
  :main grinder-clj.core
  ;; Implicit AOT of main is deprecated:
  :aot [grinder-clj.core])
