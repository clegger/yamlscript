;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.constructor is responsible for converting the YAMLScript AST
;; into a Clojure AST.

(ns yamlscript.constructor
  (:require
   [clojure.walk :as walk]
   [yamlscript.ast :refer [Lst Sym Vec]]
   [yamlscript.debug :refer [www]]))

(declare
  construct-node
  declare-undefined
  maybe-call-main
  check-let-bindings)

(defn construct
  "Construct resolved YAML tree into a YAMLScript AST."
  [node]
  (let [ctx {:lvl 0 :defn false}]
    (->> node
      (#(construct-node %1 ctx))
      (#(if (vector? %1)
          %1
          [%1]))
      (hash-map :Top)
      declare-undefined
      maybe-call-main)))

(defn construct-call [[key val]]
  (cond
    (= '=> (:Sym key)) val
    (and (:Str key) (nil? val)) key
    :else (Lst (flatten [key val]))))

(defn construct-pairs [{nodes :pairs} ctx]
  (let [pairs (vec (map vec (partition 2 nodes)))
        construct-side (fn [n c] (if (vector? n)
                                   (vec (map #(construct-node %1 c) n))
                                   (construct-node n c)))
        node (loop [pairs pairs, new []]
               (if (seq pairs)
                 (let [pairs (check-let-bindings pairs ctx)
                       [[lhs rhs] & pairs] pairs
                       lhs (construct-side lhs ctx)
                       rhs (construct-side rhs ctx)
                       rhs (or (:forms rhs) rhs)
                       new (conj new (construct-call [lhs rhs]))]
                   (recur pairs, new))
                 new))]
    node))

(defn construct-forms [{nodes :forms} ctx]
  (let [nodes (reduce
                (fn [nodes node]
                  (let [node (construct-node node ctx)]
                    (if (not= '=> (:Sym node))
                      (conj nodes node)
                      nodes)))
                [] nodes)]
    {:forms nodes}))

(defn construct-list [{nodes :Lst} ctx]
  (let [nodes (map #(construct-node %1 ctx) nodes)]
    {:Lst (-> nodes flatten vec)}))

(defn construct-node [node ctx]
  (when (vector? ctx) (throw (Exception. "ctx is a vector")))
  (let [[[key]] (seq node)
        ctx (update-in ctx [:lvl] inc)]
    (case key
      :pairs (construct-pairs node ctx)
      :forms (construct-forms node ctx)
      :Lst (construct-list node ctx)
      ,      node)))

;;------------------------------------------------------------------------------
;; Fix-up functions
;;------------------------------------------------------------------------------
(defn apply-let-bindings [lets rest ctx]
  [[(Sym "let")
    (vec
      (concat
        [(Vec (->>
                lets
                flatten
                (filter #(not= {:Sym 'let} %1))
                ;; Handle RHS is mapping
                (partition 2)
                (map #(let [[k v] %1]
                        (if (:pairs v)
                          [k (Lst (get-in
                                    (construct-pairs v ctx)
                                    [0 :Lst]))]
                          %1)))
                (mapcat identity)
                vec))]
        (construct-pairs {:pairs (mapcat identity rest)} ctx)))]])

(defn check-let-bindings [pairs ctx]
  (let [[lets rest]
        (map vec
          (split-with
            #(= 'let (get-in %1 [0 0 :Sym]))
            pairs))]
    (if (seq lets)
      (apply-let-bindings lets rest ctx)
      pairs)))

(defn get-declares [node defns]
  (let [declare (atom {})
        defined (atom {})]
    (walk/prewalk
      #(let [defn-name (when (= 'defn (get-in %1 [:Lst 0 :Sym]))
                         (get-in %1 [:Lst 1 :Sym]))
             sym-name (get-in %1 [:Sym])]
         (when defn-name (swap! defined assoc defn-name true))
         (when (and sym-name
                 (get defns sym-name)
                 (not (get @defined sym-name)))
           (swap! declare assoc sym-name true))
         %1)
      node)
    @declare))

(defn declare-undefined [node]
  (let [defn-names (map #(get-in %1 [:Lst 1 :Sym])
                     (filter #(= 'defn (get-in %1 [:Lst 0 :Sym]))
                       (rest (get-in node [:Top]))))
        defn-names (zipmap defn-names (repeat true))
        declares (map Sym
                   (keys (get-declares node defn-names)))
        form (Lst (cons (Sym 'declare) declares))]
    (if (seq declares)
      (if (= 'ns (get-in node [:Top 0 :Lst 0 :Sym]))
        (update-in node [:Top]
          #(vec (concat [(first %1)] [form] (rest %1))))
        (update-in node [:Top]
          #(vec (concat [form] %1))))
      node)))

(def call-main
  (Lst [(Sym 'apply)
        (Sym 'main)
        (Sym 'ARGV)]))

(defn maybe-call-main [node]
  (let [need-call-main (atom false)]
    (walk/prewalk
      #(let [main (and (= 'defn (get-in %1 [:Lst 0 :Sym]))
                    (= 'main (get-in %1 [:Lst 1 :Sym])))]
         (when main (reset! need-call-main true))
         %1)
      node)
    (if @need-call-main
      (update-in node [:Top] conj call-main)
      node)))

(comment
  www
  (construct :Nil)
  (construct {:do [{:Sym 'a} [{:Sym 'b} {:Sym 'c}]]})
  )
