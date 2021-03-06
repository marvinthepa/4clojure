(ns foreclojure.users
  (:use [foreclojure utils config]
        somnium.congomongo
        compojure.core
        [hiccup.page-helpers :only (link-to)]))

(def golfer-tags (into [:contributor]
                       (when (:golfing-active config)
                         [:golfer])))

(defn get-user-id [name]
  (:_id
   (fetch-one :users
              :where {:user name}
              :only [:_id])))

(def sort-by-solved-and-date (juxt (comp count :solved) :last-login))

(defn users-sort [users]
  (reverse (sort-by sort-by-solved-and-date users)))

(defn get-users []
  (let [users (from-mongo
               (fetch :users
                      :only [:user :solved :contributor]))
        sortfn  (comp count :solved)]
    (reverse (sort-by sortfn users))))

(defn golfer? [user]
  (some user golfer-tags))

(def-page users-page []
  [:div
   [:span.contributor "*"] " "
   (link-to "https://github.com/dbyrne/4clojure" "4clojure contributor")]
  [:br]
  [:table#user-table.my-table
   [:thead
    [:tr
     [:th {:style "width: 40px;"} "Rank"]
     [:th "Username"]
     [:th "Problems Solved"]]]
   (map-indexed #(vec [:tr (row-class %1)
                       [:td (inc %1)]
                       [:td
                        (when (:contributor %2)
                          [:span.contributor "* "])
                        (:user %2)]
                       [:td {:class "centered"} (count (:solved %2))]])
                (get-users))])

(defroutes users-routes
  (GET "/users" [] (users-page)))
