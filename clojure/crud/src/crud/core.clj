(ns crud.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json])
  (:use [environ.core :only [env]])
  (:gen-class))

(def cloudant-url (format "https://%s.cloudant.com/%s" (env :username) (env :db)))

(def credentials [(env :username) (env :password)])

(defn- with-auth
  "Wrap a request with authentication information."
  [verb]
  (fn [url & [opts]]
    (verb url (merge opts {:basic-auth credentials
                           :as :json}))))

(defn- with-json-body
  "Wrap a request to auto-convert the body of a request to JSON."
  [verb]
  (fn [url body & [opts]]
    (verb url (merge opts {:body (json/write-str body)
                           :content-type :json}))))

(def GET (with-auth client/get))
(def DELETE (with-auth client/delete))
(def PUT (with-json-body (with-auth client/put)))
(def POST (with-json-body (with-auth client/post)))


(defn- create-db [db-uri]
  (when (PUT db-uri {})
    db-uri))


(defn- create-doc [db-uri doc]
  (let [id (:id (:body (POST db-uri doc)))]
    (:body (GET (str db-uri "/" id)))))


(defn- update-doc [db-uri {:keys [_id _rev] :as doc}]
  (when (and _id _rev)
    (POST (str db-uri "/" _id) doc {:query-params {"rev" _rev}})
    (:body (GET (str db-uri "/" _id)))))


(defn- delete-doc [db-uri {:keys [_id _rev]}]
  (when (and _id _rev)
    (:body (DELETE (str db-uri "/" _id) {:query-params {"rev" _rev}}))))


(defn- delete-db [db-uri]
  (when db-uri
    (:body (DELETE db-uri))))


(defn -main [& args]
  (let [doc {:name "John" :age 35}]

    (println "Created " (create-db cloudant-url))

    (let [document (create-doc cloudant-url doc)]
      (println "The document's ID is" (:_id document))
      (println "The document's REV is" (:_rev document))

      (let [updated-document (update-doc cloudant-url (assoc document :age 36))]
        (println "The updated document's REV is" (:_rev updated-document))

        (println "Cleaning up:")

        (print " > doc: ")
        (println (delete-doc cloudant-url updated-document))

        (print " > db : ")
        (println (delete-db cloudant-url))))))
