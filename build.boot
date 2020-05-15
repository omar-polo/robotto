(def project 'com.omarpolo/robotto)
(def version "0.1.0-SNAPSHOT")

(set-env! :resource-paths #{"resources" "src"}
          :source-paths   #{"test"}
          :dependencies   '[[org.clojure/clojure "RELEASE"]
                            [org.clojure/data.json "1.0.0"]
                            [http-kit "2.4.0-alpha6"]
                            [org.clojure/core.async "1.0.567"]
                            [adzerk/boot-test "RELEASE" :scope "test"]])

(task-options!
 pom {:project     project
      :version     version
      :description "telegram bot library"
      :url         "https://projects.omarpolo.com/robotto"
      :scm         {:url "https://git.omarpolo.com/robotto.git"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask build
  "Build and install the project locally."
  []
  (comp (pom) (jar) (install)))

(require '[adzerk.boot-test :refer [test]])
