(defproject simple-openapi "0.1.0"
  :description "A reusable OpenAPI 3.1.1 + Swagger UI library for Ring/Compojure applications"
  :url "https://github.com/formcept/simple-openapi"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring/ring-core "1.12.1"]
                 [ring/ring-jetty-adapter "1.12.1"]
                 [ring/ring-json "0.5.1"]
                 [compojure "1.7.1"]
                 [cheshire "5.12.0"]
                 [org.slf4j/slf4j-simple "2.0.13"]]
  :profiles {:demo {:main openapi.demo.core}}
  :main openapi.demo.core)
