(defproject com.adva/netemu "0.1.0-SNAPSHOT"
  :description ""
  :url "https://github.com/advaoptical/netemu"

  :dependencies
  [[org.clojure/clojure "1.10.1"]
   [org.clojure/algo.generic "0.1.3"]]

  :source-paths ["src/main/clj"]
  :java-source-paths ["src/main/java"]

  :pom-plugins
  [[org.opendaylight.yangtools/yang-maven-plugin "4.0.1"
    {:dependencies

     [:dependency
      [:groupId "org.opendaylight.mdsal"]
      [:artifactId "maven-sal-api-gen-plugin"]
      [:version "3.0.3"]
      [:type "jar"]]

     :executions
     [:execution
      [:goals [:goal "generate-sources"]]

      [:configuration
       [:inspectDependencies true]
       [:yangFilesRootDir "src/main/resources/META-INF/yang"]

       [:codeGenerators
        [:generator
         [:outputBaseDir "build/mdsal"]
         [:codeGeneratorClass
          "org.opendaylight.mdsal.binding.maven.api.gen.plugin"
          ".CodeGeneratorImpl"]]]]]}]])
