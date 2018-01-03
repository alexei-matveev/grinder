; Copyright (C) 2012 - 2013 Philip Aston
; All rights reserved.
;
; This file is part of The Grinder software distribution. Refer to
; the file LICENSE which is part of The Grinder distribution for
; licensing details. The Grinder distribution is available on the
; Internet at http://grinder.sourceforge.net/
;
; THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
; "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
; LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
; FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
; COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
; INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
; (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
; SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
; HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
; STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
; ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
; OF THE POSSIBILITY OF SUCH DAMAGE.

;;
;; This file is for convenience of  Emacs & Cider workflows.  The file
;; is not required of the Web Console itself as the "play" in the name
;; suggests.
;;
(ns net.grinder.console.service.play
  "Start and stop the console from the REPL."
  (:require
   [clojure.tools [logging :as log]])
  (:import
   java.util.Locale                     ; Locale/getDefault
   org.slf4j.LoggerFactory              ; LoggerFactory/getLogger
   net.grinder.console.common.ResourcesImplementation
   net.grinder.console.ConsoleFoundation
   net.grinder.translation.impl.TranslationsSource))

(defonce ^:private stopper (atom nil))

;; Stopper function happens to also reset the atom to nil again. See
;; below where it is constructed ...
(defn stop []
  (if-let [stopper-fn @stopper]
    (stopper-fn)))

;; Execute (start) and point  your browser to http://localhost:6373/ui
;; or to http://localhost:6373/version to  verify. The run() method of
;; the  ConsoleFoundation  does  not  return anything,  so  the  value
;; computed by the future is not very usefull.
(defn start []
  ;; This should be idempotent:
  (stop)
  ;; See main() in Console.java for the reference startup sequence ...
  (let [resources (ResourcesImplementation. ConsoleFoundation/RESOURCE_BUNDLE)
        logger (LoggerFactory/getLogger "test")
        headless true
        translations (-> (TranslationsSource.)
                         (.getTranslations (Locale/getDefault)))
        cf (ConsoleFoundation. resources
                               translations
                               logger
                               headless)]
    (reset! stopper
            (fn []
              (.shutdown cf)
              ;; Should we rather reset the atom in "stop"?
              (reset! stopper nil)))
    (future
      (try
        (.run cf)
        (catch Exception e (log/error e "ConsoleFoundation failed"))))))


