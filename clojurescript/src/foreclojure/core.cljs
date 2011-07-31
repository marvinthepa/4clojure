(ns foreclojure.core)

(def $ (js* "$"))
(def document (js* "document"))
(def ace (js* "ace"))
(def js-require (js* "require"))
(def set-timeout (js* "setTimeout"))
(def this (js* "this"))
(def confirm (js* "confirm"))

(defn js-map
  "recursively converts a map/vector (containing other maps or vectors as values)
  to a javascript object/array"
  [cljmap]
  (cond (vector? cljmap) (apply array (map js-map cljmap))
        (map? cljmap) (let [out (js-obj)
                            mapf #(aset out (name (first %)) (js-map (second %)))]
                        (doall (map mapf cljmap))
                        out)
        true cljmap))

(defn configure-data-tables []
  (let [problem-table ($ "#problem-table")
        unapproved-problems ($ "#unapproved-problems")
        user-table ($ "#user-table")]
    (.dataTable problem-table
                (js-map
                  {"iDisplayLength" 25
                   "aaSorting" [[3 "desc"]]
                   "aoColumns" [nil nil nil nil {"sType" "string"}]}))
    (.dataTable unapproved-problems
                (js-map {"iDisplayLength" 25
                         "aaSorting" [[2 "desc"]]
                         "aoColumns" [nil nil nil]}))
    (.dataTable user-table
                (js-map {"iDisplayLength" 25
                         "aaSorting" [[0 "asc"]]
                         "aoColumns" [nil nil nil]}))))

(defn configure-golf []
  (let []
    (. ($ "#graph-link") (show))
    (. ($ "#golfgraph") (hide))
    (.click ($ "#graph-link")
            (fn []
              (let [text (. ($ "#graph-link") (html))]
                (.toggle ($ "#code-div") "fast" (fn []))
                (.toggle ($ "#golfgraph") "fast" (fn []))
                (if (= text "View Chart")
                  (.html ($ "#graph-link") "View Code")
                  (.html ($ "#graph-link") "View Chart")))))))

(defn set-icon-color
  ([element color] (set-icon-color element color 0))
  ([element color timeout]
   (set-timeout (fn [] (set! (.src element)
                             (str "/images/" color "light.png")))
                timeout)))

(defn change-to-code-view []
  (.show ($ "#code-div") "fast")
  (.hide ($ "#golfgraph") "fast")
  (.html ($ "#graph-link") "View Chart"))

(def wait-time-per-item 500)
(def wait-time wait-time-per-item) ; TODO change to local val
(def cont true)
(def editor)
(def session)

(defn click-handler []
  (let [text (. session (getValue))
        id (.attr ($ "#id") "value")
        images (.find ($ ".testcases") "img")
        animation-time 800
        before-send (fn [data]
                      (set! cont true)
                      (let [anim (fn anim [high]
                                   (when cont
                                     (.animate images
                                               (js-map {"opacity" (if high 0.0 0.1)})
                                               animation-time)
                                     (set-timeout (fn [] (anim (not high))))))]
                        (.each images (fn [index element]
                                        (set-icon-color element "blue")))
                        (.text ($ "#message-text")
                               "Executing unit tests...")
                        (set-timeout change-to-code-view 0)
                        (set-timeout (fn [] (anim false)))))
        error (fn [data string error]
                (.text
                  ($ "#message-text")
                  (str "An Error occured: " error)))
        success (fn [data]
                  (let [failing-test (.failingTest data)
                        get-color-for (fn [index]
                                        (if (= index failing-test)
                                          "red" "green"))
                        test-was-executed (fn [index]
                                            (<= index failing-test))
                        set-color (fn [index element]
                                    (let [color (get-color-for index)]
                                      (set! wait-time (* wait-time-per-item (inc index)))
                                      (set-icon-color element color wait-time)))
                        set-messages (fn []
                                       (.html ($ "#message-text") (.message data))
                                       (.html ($ "#golfgraph") (.golfChart data))
                                       (.html ($ "#golfscore") (.golfScore data))
                                       (configure-golf))
                        stop-animation (fn []
                                         (set! cont false)
                                         (.stop images true)
                                         (.css images (js-map {"opacity" 1.0})))]
                    (set-timeout stop-animation 0)
                    (.each
                      (.filter images test-was-executed)
                      set-color)
                    (set-timeout set-messages wait-time)))]
    (.ajax $ (js-map
               {"type" "POST"
                "url" (str "/rest/problem/" id)
                "dataType" "json"
                "data" (js-map {"id" id "code" text})
                "timeout" 20000 ; default clojail timeout is 10000
                "beforeSend" before-send
                "success" success
                "error" error}))
    false))

(defn configure-code-box []
  (let [old-box ($ "#code-box")]
    (if-not (zero? (.length old-box)) 
      (.replaceWith old-box
                    (str "<div id=\"code-div\">"
                         "<pre id=\"editor\">"
                         (. old-box (val))
                         "</pre></div>"
                         "<input type=\"hidden\" value=\"blank\" name=\"code\" id=\"code\">"))) 
    (when-not (zero? (.length ($ "#run-button")))
      (set! editor (.edit ace "editor"))
      (set! session (. editor (getSession)))
      (let [clojure-mode (.Mode (js-require "ace/mode/clojure"))]
        (.setTheme editor "ace/theme/textmate")
        (doto session
          (.setMode (clojure-mode.))
          (.setUseSoftTabs true)
          (.setTabSize 2))
        (set!
          (-> document
            (.getElementById "editor")
            .style
            .fontSize)
          13)
        (.click ($ "#run-button") click-handler)))))

(defn on-document-ready []
  (configure-data-tables)
  (configure-code-box)
  (configure-golf)

  (.live ($ "form#run-code button#approve-button")
         "click"
         (fn [e]
           (. e (preventDefault))
           (when (confirm "Are you sure you want to mark this problem as approved?")
             (-> (.parents ($ this) "form")
               (.attr "action" "/problem/approve")
               (.submit)))))

  (.live ($ "form#run-code button#reject-button")
         "click"
         (fn [e]
           (. e (preventDefault))
           (when (confirm "Are you sure you want to reject this problem? It will be permanently deleted.")
             (-> (.parents ($ this) "form")
               (.attr "action" "/problem/reject")
               (.submit)))))

  (.live ($ "form#run-code button#edit-button")
         "click"
         (fn [e]
           (. e (preventDefault))
           (-> (.parents ($ this) "form")
             (.attr "action" "/problem/edit")
             (.submit)))))

(-> ($ document)
  (.ready on-document-ready))
