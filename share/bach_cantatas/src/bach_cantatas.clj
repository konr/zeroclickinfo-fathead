(ns bach-cantatas
  (:require [net.cgrand.enlive-html :as html]))


(def base-url "http://bach-cantatas.com")

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn middleware [function]
  #(do (function %) %))

;; Index page

(defn index-page
  ([] (index-page 1))
  ([n] (-> base-url (str "/") (str "BWV" n ".htm"))))

