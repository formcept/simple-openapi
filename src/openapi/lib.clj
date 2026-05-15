(ns openapi.lib
  (:require
   [cheshire.core :as json]
   [ring.middleware.json :refer [wrap-json-body wrap-json-response]]))

;; =============================================================================
;; OpenAPI Library - Core Utilities
;; =============================================================================
;;
;; This namespace provides reusable utilities for integrating OpenAPI/Swagger UI
;; into Ring/Compojure applications.
;;
;; Usage:
;;   (require '[openapi.lib :refer [add-swagger-ui]])
;;   (def app (-> routes
;;                (add-swagger-ui {:title "My API" :version "1.0.0"})
;;                (wrap-json-body {:keywords? true})
;;                wrap-json-response))
;; =============================================================================

;; =============================================================================
;; Response Helpers
;; =============================================================================

(defn json-response
  "Create a JSON response with optional status code.
   
   Args:
     body - Response body (will be JSON serialized)
     status - HTTP status code (default: 200)
   
   Returns:
     Ring response map"
  ([body]
   (json-response body 200))

  ([body status]
   {:status status
    :headers {"Content-Type" "application/json"}
    :body (if (string? body) body (json/generate-string body))}))

(defn success-response
  "Create a 200 OK JSON response"
  [body]
  (json-response body 200))

(defn created-response
  "Create a 201 Created JSON response"
  [body]
  (json-response body 201))

(defn error-response
  "Create an error JSON response with given status code"
  [status message]
  (json-response {:error message} status))

;; =============================================================================
;; OpenAPI Spec Utilities
;; =============================================================================

(defn merge-schemas
  "Merge user-provided schemas with existing ones"
  [& schema-maps]
  (apply merge schema-maps))

(defn merge-paths
  "Merge user-provided path definitions with existing ones"
  [& path-maps]
  (apply merge path-maps))

(defn build-openapi-spec
  "Build a complete OpenAPI 3.1.1 specification.
   
   Args:
     config - Map with keys:
       :title - API title
       :version - API version
       :description - API description (optional)
       :servers - Vector of server objects (optional)
     paths - Map of OpenAPI path definitions
     schemas - Map of JSON Schema definitions
     tags - Vector of tag objects (optional)
   
   Returns:
     OpenAPI 3.1.1 specification map"
  [{:keys [title version description servers] :as config}
   paths
   schemas
   {:keys [tags] :or {tags []}}]
  
  (cond->
    {:openapi "3.1.1"
     :info
     (cond->
       {:title title
        :version version}
       description (assoc :description description))
     :paths paths
     :components {:schemas schemas}}
    
    servers (assoc :servers servers)
    (seq tags) (assoc :tags tags)))

;; =============================================================================
;; Swagger UI Helpers
;; =============================================================================

(defn generate-swagger-ui-html
  "Generate Swagger UI HTML.
   
   Args:
     openapi-json-path - Path to serve OpenAPI JSON (e.g., \"/api/openapi.json\")
     swagger-ui-path - Path where Swagger UI is served (e.g., \"/api/docs\")
   
   Returns:
     HTML string"
  [openapi-json-path]
  
  (str
    "<!DOCTYPE html>"
    "<html>"
    "<head>"
    "<title>Swagger UI</title>"
    "<link rel='stylesheet' href='https://unpkg.com/swagger-ui-dist/swagger-ui.css' />"
    "<style>"
    "  body { margin: 0; padding: 0; }"
    "</style>"
    "</head>"
    "<body>"
    "<div id='swagger-ui'></div>"
    "<script src='https://unpkg.com/swagger-ui-dist/swagger-ui-bundle.js'></script>"
    "<script src='https://unpkg.com/swagger-ui-dist/swagger-ui-standalone-preset.js'></script>"
    "<script>"
    "  window.onload = function() {"
    "    window.ui = SwaggerUIBundle({"
    "      url: '" openapi-json-path "',"
    "      dom_id: '#swagger-ui',"
    "      deepLinking: true,"
    "      presets: ["
    "        SwaggerUIBundle.presets.apis,"
    "        SwaggerUIStandalonePreset"
    "      ],"
    "      layout: 'StandaloneLayout'"
    "    })"
    "  }"
    "</script>"
    "</body>"
    "</html>"))

;; =============================================================================
;; Middleware Factory
;; =============================================================================

(defn create-swagger-middleware
  "Create middleware that serves Swagger UI and OpenAPI JSON.
   
   Args:
     handler - The next handler in the chain
     options - Map with keys:
       :openapi-spec - The OpenAPI specification (as a map)
       :swagger-ui-path - Path to serve Swagger UI at (default: \"/api/docs\")
       :openapi-json-path - Path to serve OpenAPI JSON at (default: \"/api/openapi.json\")
   
   Returns:
     Ring middleware function"
  [handler {:keys [openapi-spec swagger-ui-path openapi-json-path]
            :or {swagger-ui-path "/api/docs"
                 openapi-json-path "/api/openapi.json"}}]
  
  (fn [request]
    (condp = (:uri request)
      
      swagger-ui-path
      {:status 200
       :headers {"Content-Type" "text/html; charset=utf-8"}
       :body (generate-swagger-ui-html openapi-json-path)}
      
      openapi-json-path
      {:status 200
       :headers {"Content-Type" "application/json; charset=utf-8"}
       :body (json/generate-string openapi-spec)}
      
      (handler request))))

(defn wrap-swagger-ui
  "Middleware wrapper to add Swagger UI and OpenAPI JSON endpoints.
   
   Usage:
     (-> routes
         (wrap-swagger-ui {:openapi-spec spec
                          :swagger-ui-path \"/docs\"
                          :openapi-json-path \"/openapi.json\"})
         (wrap-json-body {:keywords? true})
         wrap-json-response)
   
   Args:
     handler - Ring handler
     options - Configuration options (see create-swagger-middleware)
   
   Returns:
     Wrapped handler"
  [handler options]
  (create-swagger-middleware handler options))
