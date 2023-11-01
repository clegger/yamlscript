(defproject yamlscript/cli "0.1.0"
  :description "YAMLScript Command Line Tool"

  :url "https://github.com/yaml/yamlscript"

  :license
  {:name "MIT"
   :url "https://opensource.org/license/mit/"}

  :scm
  {:name "git"
   :url "https://github.com/yaml/yamlscript"
   :tag "clojure"
   :dir ".."}

  :dependencies
  [[org.clojure/clojure "1.11.1"]
   [org.clojure/data.json "2.4.0"]
   [org.clojure/tools.cli "1.0.219"]
   [org.babashka/sci "0.8.40"]
   [clj-commons/clj-yaml "1.0.27"]
   [yamlscript/core "0.1.0"]]

  :main ^:skip-aot yamlscript.cli

  :target-path "target/%s"

  :plugins
  [[lein-exec "0.3.7"]
   #_[io.github.borkdude/lein-lein2deps "0.1.0"]]

  :prep-tasks [["compile"] ["javac"]]

  :java-source-paths ["src"]

  :profiles
  {:uberjar
   {:aot [yamlscript.cli]
    :main yamlscript.cli
    :global-vars
    {*assert* false
     *warn-on-reflection* true}
    :jvm-opts
    ["-Dclojure.compiler.direct-linking=true"
     "-Dclojure.spec.skip-macros=true"]}}

  :repositories [["public-github" {:url "git://github.com"}]]

  :global-vars {*warn-on-reflection* true})
