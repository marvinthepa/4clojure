(ns foreclojure.core)

(def $ (js* "$"))
(def document (js* "document"))
(def alert (js* "alert"))
(def ace (js* "ace"))
(def js-require (js* "require"))

(defn js-map
    "makes a javascript map from a clojure one"
  ; TODO make this work recursively, with vectors/lists transformed to javascript arrays
    [cljmap]
    (let  [out (js-obj)]
          (doall
            (map #(aset out
                        (name (first %)) (second %))
                 cljmap))
          out))


(defn configure-data-tables []
  (let [problem-table ($ "#problem-table")
        unapproved-problems ($ "#unapproved-problems")
        user-table ($ "user-table")]
    (.dataTable problem-table
                (js-map
                  {"iDisplayLength" 25
                   "aaSorting" (array (array 3 "desc"))
                   "aoColumns" (array nil nil nil nil, (js-map {"sType" "string"}))}))
    (.dataTable unapproved-problems
                (js-map {"iDisplayLength" 25
                         "aaSorting" (array (array 2 "desc"))
                         "aoColumns" (js* "[null, null, null]")}))
    (.dataTable user-table
                (js-map {"iDisplayLength" 25
                         "aaSorting" (array (array 0 "desc"))
                         "aoColumns" (js* "[null, null, null]")}))))

(defn configure-code-box []
  (let [old-box ($ "#code-box")]
    (.replaceWith old-box
                  (str "<div id=\"code-div\">"
                       "<pre id=\"editor\">"
                       (. old-box (val))
                       "</pre></div>"
                       "<input type=\"hidden\" value=\"blank\" name=\"code\" id=\"code\">"))
    (when-not (zero? (.length ($ "#run-button"))) 
      (let [editor (.edit ace "editor")
            clojure-mode (.Mode (js-require "ace/mode/clojure")) 
            session (.getSession editor ())
            click-handler (fn []
                            (doto ($ "#code")
                              (.val (. session (getValue))))
                            true) ; TODO
            ]
        (.setTheme editor "ace/theme/textmate")
        (.click ($ "#run-button") click-handler) 
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
        ))))

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
                       (.html ($ "#graph-link") "View Chart")
                       ))))))

(defn on-document-ready []
  (configure-data-tables)
  (configure-code-box)
  (configure-golf))

(-> ($ document)
  (.ready on-document-ready))
