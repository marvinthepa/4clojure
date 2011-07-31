to compile:

- start a clojurescript repl in the base 4clojure directory
- require the clojurescript compiler
    (require '[cljs.closure :as cljsc])
- to compile:
    (cljsc/build "clojurescript/src" {:output-dir "resources/public/script/cljs/out" :output-to "resources/public/script/cljs/foreclojure.js"})

This will compile the non-minified version, which is compatible with the current html generated in by html-doc in src/foreclojure/utils.clj.
