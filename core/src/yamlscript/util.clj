;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; Helper utility functions.

(ns yamlscript.util
  (:require
   [babashka.fs
    :refer [cwd]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [yamlscript.debug
    :refer [www]]))

(defonce build-vstr (atom nil))

(defn abspath
  ([path] (abspath path (str (cwd))))
  ([path base]
   (if (-> path io/file .isAbsolute)
     path
     (.getAbsolutePath (io/file (abspath base) path)))))

(defmacro cond-lets
  {:style/indent [0]}
  [& clauses]
  (when clauses
    `(if-lets ~(first clauses)
       ~(if (next clauses)
          (second clauses)
          (die "Odd number of forms"))
       (cond-lets ~@(nnext clauses)))))

(defn die
  ([msg] (throw (Exception. ^String msg)))
  ([msg info] (throw (ex-info msg info))))

(defn dirname
  [path]
  (->
    path
    io/file
    .getParent
    (or ".")))

(defn get-yspath [base]
  (let [yspath (or
                 (get (System/getenv) "YSPATH")
                 (when (re-matches #"/NO-NAME$" base) (str (cwd)))
                 (->
                   base
                   dirname
                   abspath))
        _ (when-not yspath
            (die "YSPATH environment variable not set"))]
    (map abspath (str/split yspath #":"))))

(defmacro if-lets
  ([bindings then]
   `(if-lets ~bindings ~then nil))
  ([bindings then else]
   (if (seq bindings)
     `(if-let [~(first bindings) ~(second bindings)]
        (if-lets ~(drop 2 bindings) ~then ~else)
        ~else)
     then)))

(defmacro when-lets
  ([bindings & body]
   (if (seq bindings)
     `(when-let [~(first bindings) ~(second bindings)]
        (when-lets ~(drop 2 bindings) ~@body))
     `(do ~@body))))

(defn www [& o] (apply debug/www o))

(comment
  www)
