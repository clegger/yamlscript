;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.re library defines the regex parts that are used to match
;; YAMLScript ysexpr tokens.
;;
;; It defines an `re` function that takes a regex template and expands the
;; interpolations to create a regex pattern.

(ns yamlscript.re
  (:require
   [clojure.string :as str]
   [yamlscript.debug :refer [www]])
  (:refer-clojure :exclude [char]))

(defn re [rgx]
  (loop [rgx (str rgx)]
    (let [match (re-find #"\$([a-zA-Z]+)" rgx)]
      (if match
        (let [var (second match)
              val (var-get
                    (resolve
                      (symbol (str "yamlscript.re/" var))))
              rgx (str/replace
                    rgx
                    (re-pattern (str #"\$" var #"(?![a-zA-Z])"))
                    (str/re-quote-replacement val))]
          (recur rgx))
        (re-pattern rgx)))))

(def char #"(?x)
            \\
            (?:
              newline |
              space |
              tab |
              formfeed |
              backspace |
              return |
              .
            )")                            ; Character token
(def comm #";.*(?:\n|\z)")                 ; Comment token
(def ignr #"(?x)
            (?:                            # Ignorables
              |                              # Empty
              \#\!.*\n? |                    # hashbang line
              [\s,]+    |                    # whitespace, commas,
              ;.*\n?                         # comments
            )")
(def inum #"-?\d+")                        ; Integer literal token
(def fnum (re #"$inum\.\d*(?:e$inum)?"))   ; Floating point literal token
(def xnum (re #"(?:$fnum|$inum)"))         ; Numeric literal token
(def xsym #"(?:\=\~)")                     ; Special operator token
(def osym #"(?:[-+*/%<=>~|&.]{1,3})")      ; Operator symbol token
(def anon #"(?:\\\()")                     ; Anonymous fn start token
(def narg #"(?:%\d+)")                     ; Numbered argument token
(def regx #"(?x)                           # Regular expression
            / (?=\S)                         # opening slash
            (?:
              \\. |                          # Escaped char
              [^\\\/\n]                      # Any other char
            )+/                              # Ending slash
            ")
(def dstr #"(?x)
            \"(?:                          # Double quoted string
              \\. |                          # Escaped char
              [^\\\"]                        # Any other char
            )*\"                             # Ending quote
            ")
(def sstr #"(?x)
            '(?:                          # Single quoted string
              '' |                           # Escaped single quote
              [^']                           # Any other char
            )*'                              # Ending quote
            ")
(def pnum #"(?:\d+)")                      ; Positive integer
(def anum #"[a-zA-Z0-9]")                  ; Alphanumeric
(def symw (re #"(?:$anum+(?:-$anum+)*)"))  ; Symbol word
(def pkey (re #"(?:$symw|$pnum|$dstr|$sstr)"))   ; Path key
(def path (re #"(?:$symw(?:\.$pkey)+)"))   ; Lookup path
(def keyw (re #"(?:\:$symw)"))             ; Keyword token
                                           ; Clojure symbol
(def csym #"(?:[-a-zA-Z0-9_*+?!<=>]+(?:\.(?=\ ))?)")
(def ysym (re #"(?:$symw[?!.]?)"))         ; YS symbol token
(def dsym (re #"(?:$symw=)"))              ; YS symbol with default
(def nspc (re #"(?:$symw(?:\:\:$symw)+)")) ; Namespace symbol
(def fsym (re #"(?:(?:$nspc|$symw)\/$ysym)"))  ; Fully qualified symbol
                                           ; Symbol followed by paren
(def psym (re #"(?:(?:$fsym|$ysym)\()"))
(def esym (re #"(?:\*$symw\*)"))           ; Earmuff symbol

(def defk (re #"^$symw +=$"))              ; Pair key for def/let call
(def dfnk (re #"^defn ($ysym)(?:\((.*)\))?$"))  ; Pair key for defn call

; Balanced parens
(def bpar #"(?x)
            (?:\(
              [^)(]*(?:\(
                [^)(]*(?:\(
                  [^)(]*(?:\(
                    [^)(]*(?:\(
                      [^)(]*(?:\(
                        [^)(]*
                      \)[^)(]*)*
                    \)[^)(]*)*
                  \)[^)(]*)*
                \)[^)(]*)*
              \)[^)(]*)*
            \))
          ")

(comment
  www
  )
