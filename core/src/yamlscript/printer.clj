;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.printer is responsible for serializing YAMLScript Clojure AST
;; into Clojure code.

(ns yamlscript.printer
  (:use yamlscript.debug)
  (:require
   [clojure.string :as str]
   [zprint.core :as zp])
  (:refer-clojure :exclude [print]))

(def string-escape
  {\\ "\\\\"
   \" "\\\""
   \newline "\\n"})

(defn pr-string [s]
  (-> s
    (str/escape string-escape)))

(defn print-node [node]
  (let [node (if (keyword? node)
               {node true}
               node)
        [type val] (first node)]
    (case type
      :Empty ""
      :Lst (str
             "("
             (str/join " " (map print-node val))
             ")")
      :Vec (str
             "["
             (str/join " " (map print-node val))
             "]")
      :Map (str
             "{"
             (str/join ", " (->> val
                              (partition 2)
                              (map #(str
                                      (print-node (first %))
                                      " "
                                      (print-node (second %))))))
             "}")
      :Str (str \" (pr-string val) \")
      :Chr (str "\\" val)
      :Spc (str/replace val #"::" ".")
      :Sym (str val)
      :Tok (str val)
      :Key (str val)
      :Int (str val)
      :Flt (str val)
      :Bln (str val)
      :Nil "nil"
      ,     (throw
              (Exception. (str "Unknown AST node type:"
                            node))))))

(defn pretty-format [code]
  (zp/zprint-str
    code
    {:style :community
     :parse-string-all? true}))

(defn print
  "Render a YAMLScript AST as Clojure code."
  [node]
  (let [list (:Top node)
        code (->> list
               (map print-node)
               (str/join "\n")
               pretty-format)]
    code))

(comment
  (print :Empty)
  (print
    {:Lst [{:Sym 'a} {:Sym 'b} {:Sym 'c}]})
  (print {:Map [{:Str "foo"} {:Str "\\a"}]})
  )
