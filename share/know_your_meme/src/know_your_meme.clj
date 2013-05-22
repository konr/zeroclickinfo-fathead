(ns know-your-meme
  (:require [net.cgrand.enlive-html :as html]))


(def base-url "http://knowyourmeme.com")

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn middleware [function]
  #(do (function %) %))

;; Reference

(def memes-processed (atom 0))
(def pages-fetched   (atom 0))
(def total-memes (atom 0))
(def total-pages (atom 0))

(defn declare-index-fetched []
  (let [n (swap! pages-fetched inc)]
    (println (format "Fetched %d index%s out of %d"
                     n (if (= 1 n) "" "es") @total-pages))))

(defn declare-meme-processed []
  (let [n (swap! memes-processed inc)]
    (println (format "Processed %d meme%s out of %d"
                     n (if (= 1 n) "" "s") @total-memes))))

;; Index page

(defn index-page
  ([] (index-page 1))
  ([n] (-> base-url
           (str "/search?")
           (str "q=status%3Aconfirmed+category%3Ameme")
           (str "&page=" n))))

(defn clickable-photos [url]
  (declare-index-fetched)
  (map (comp :href :attrs) (html/select (fetch-url url) [:table.entry_list :a.photo])))

(defn number-of-pages []
  (->> [:div.pagination :a] (html/select (fetch-url (index-page))) (map (comp first :content))
       (filter #(re-find #"^[0-9]*$" %)) (map #(Integer. %)) (apply max)))

;; Meme page

(defn extract-info-from-meme [path]
  (let [url (str base-url path)
        data (fetch-url url)
        title (->> [:section.info :a]     (html/select data) first :content first)
        about (->> [:section.bodycopy :p] (html/select data) first :content (map html/text) (apply str))]
    (declare-meme-processed)
    {:url url
     :title title
     :about about}))

(defn in-ddg-format [{:keys [url title about] :as all}]
  (->>
   [;; REQUIRED: full article title
    title

    ;; REQUIRED:
    ;;A for article.
    ;;D for disambiguation page.
    ;;R for redirect.
    "A"

    ;; Only for redirects
    ""

    ;;Ignore.
    ""

    ;; You can put the article in multiple categories, and category pages will be created automatically.
    ;; E.g.: http://duckduckgo.com/c/Procedural_programming_languages
    ;; You would do: Procedural programming languages\\n
    ;; You can have several categories, separated by an escaped newline.
    ;; Categories should generally end with a plural noun.
    ""


    ;; Ignore.
    ""

    ;; You can reference related topics here, which get turned into links in the Zero-click Info box.
    ""

    ;; Ignore.
    ""

    ;; You can add external links that get put first when this article comes out.
    ;; The canonical example is an official site, which looks like:
    ;; [$url Official site]\\n
    ;; You can have several, separated by an escaped newline though only a few will be used.
    ;; You can also have before and after text or put multiple links in one like this.
    ;; Before text [$url link text] after text [$url2 second link].\\n
    ""

    ;; Ignore.
    ""

    ;; You can reference an external image that we will download and reformat for display.
    ;; You would do: [[Image:$url]] #FIX
    ""

    ;; This is the snippet info.
    ;; It should generally be ONE readable sentence, ending in a period.
    about

    ;; This is the full URL for the source.
    ;; If all the URLs are relative to the main domain,
    ;; this can be relative to that domain.
    url
    ]
   (interpose "\t") (apply str)))

;; Core

(defn extract-all-confirmed-memes-from-KYM [& _]
  (->> (number-of-pages) ((middleware #(reset! total-pages %)))
       range (map inc)
       (map #(-> % index-page clickable-photos)) flatten
       ((middleware #(reset! total-memes (count %))))
       (map #(-> % extract-info-from-meme in-ddg-format))
       (interpose "\n") (apply str) (spit "output.txt")))

(def -main extract-all-confirmed-memes-from-KYM)
