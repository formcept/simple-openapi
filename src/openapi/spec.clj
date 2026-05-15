(ns openapi.spec
  (:require
   [clojure.string :as str]))

;; =============================================================================
;; OpenAPI Specification Building Utilities
;; =============================================================================
;;
;; Provides declarative helpers for building OpenAPI 3.1.1 specifications.
;; Reduces boilerplate when defining API paths, schemas, and parameters.
;; =============================================================================

;; =============================================================================
;; Schema Builders
;; =============================================================================

(defn string-schema
  "Create a string schema with optional constraints"
  [& {:keys [format min-length max-length pattern enum description]
      :as opts}]
  (cond->
    {:type "string"}
    format (assoc :format format)
    min-length (assoc :minLength min-length)
    max-length (assoc :maxLength max-length)
    pattern (assoc :pattern pattern)
    enum (assoc :enum enum)
    description (assoc :description description)))

(defn integer-schema
  "Create an integer schema with optional constraints"
  [& {:keys [minimum maximum enum description]
      :as opts}]
  (cond->
    {:type "integer"}
    minimum (assoc :minimum minimum)
    maximum (assoc :maximum maximum)
    enum (assoc :enum enum)
    description (assoc :description description)))

(defn number-schema
  "Create a number schema with optional constraints"
  [& {:keys [minimum maximum enum description]
      :as opts}]
  (cond->
    {:type "number"}
    minimum (assoc :minimum minimum)
    maximum (assoc :maximum maximum)
    enum (assoc :enum enum)
    description (assoc :description description)))

(defn boolean-schema
  "Create a boolean schema"
  [& {:keys [description] :as opts}]
  (cond->
    {:type "boolean"}
    description (assoc :description description)))

(defn array-schema
  "Create an array schema"
  [item-schema & {:keys [min-items max-items description]
                  :as opts}]
  (cond->
    {:type "array"
     :items item-schema}
    min-items (assoc :minItems min-items)
    max-items (assoc :maxItems max-items)
    description (assoc :description description)))

(defn object-schema
  "Create an object schema with properties and required fields"
  [properties & {:keys [required additional-properties description]
                 :or {additional-properties false}
                 :as opts}]
  (cond->
    {:type "object"
     :properties properties}
    required (assoc :required required)
    (not additional-properties) (assoc :additionalProperties false)
    description (assoc :description description)))

;; =============================================================================
;; Parameter Builders
;; =============================================================================

(defn path-parameter
  "Create a path parameter definition"
  [name schema & {:keys [description required]
                  :or {required true}
                  :as opts}]
  {:name name
   :in "path"
   :required required
   :schema schema
   :description description})

(defn query-parameter
  "Create a query parameter definition"
  [name schema & {:keys [description required]
                  :as opts}]
  (cond->
    {:name name
     :in "query"
     :schema schema}
    required (assoc :required required)
    description (assoc :description description)))

(defn header-parameter
  "Create a header parameter definition"
  [name schema & {:keys [description required]
                  :as opts}]
  (cond->
    {:name name
     :in "header"
     :schema schema}
    required (assoc :required required)
    description (assoc :description description)))

;; =============================================================================
;; Response Builders
;; =============================================================================

(defn json-content
  "Create application/json content with a schema"
  [schema]
  {"application/json" {:schema schema}})

(defn response
  "Create an OpenAPI response definition"
  [description & {:keys [schema headers]
                  :as opts}]
  (cond->
    {:description description}
    schema (assoc :content (json-content schema))
    headers (assoc :headers headers)))

;; =============================================================================
;; Operation Builders
;; =============================================================================

(defn operation
  "Create an OpenAPI operation (GET, POST, etc.)
   
   Args:
     method - HTTP method name (e.g., \"GET\", \"POST\")
     summary - Short description
     options - Map with keys:
       :description - Longer description
       :operation-id - Unique operation identifier
       :tags - Vector of tag names
       :parameters - Vector of parameter definitions
       :request-schema - Schema for request body
       :responses - Map of status codes to response definitions
   
   Returns:
     OpenAPI operation map"
  [method summary & {:keys [description operation-id tags parameters
                            request-schema responses]
                     :as opts}]
  (cond->
    {:summary summary}
    description (assoc :description description)
    (or operation-id summary) (assoc :operationId (or operation-id
                                                      (str/replace summary #"\s+" "")))
    tags (assoc :tags tags)
    parameters (assoc :parameters parameters)
    request-schema (assoc :requestBody
                          {:required true
                           :content (json-content request-schema)})
    responses (assoc :responses responses)))

(defn get-op
  "Shorthand for creating a GET operation"
  [summary opts]
  (operation "GET" summary opts))

(defn post-op
  "Shorthand for creating a POST operation"
  [summary opts]
  (operation "POST" summary opts))

(defn put-op
  "Shorthand for creating a PUT operation"
  [summary opts]
  (operation "PUT" summary opts))

(defn patch-op
  "Shorthand for creating a PATCH operation"
  [summary opts]
  (operation "PATCH" summary opts))

(defn delete-op
  "Shorthand for creating a DELETE operation"
  [summary opts]
  (operation "DELETE" summary opts))

;; =============================================================================
;; Path Item Builders
;; =============================================================================

(defn path-item
  "Create an OpenAPI path item with multiple operations
   
   Usage:
     (path-item
       (get-op \"Get user\" {:parameters [...] :responses {...}})
       (post-op \"Create user\" {...}))
   
   Args:
     operations - Variable number of operation maps
   
   Returns:
     Map of HTTP methods to operation definitions"
  [& operations]
  (into {}
        (mapcat
          (fn [op-map]
            (for [[method op] op-map]
              [method op]))
          operations)))

;; =============================================================================
;; Complete Spec Builder
;; =============================================================================

(defn openapi-spec
  "Build a complete OpenAPI 3.1.1 specification
   
   Usage:
     (openapi-spec
       {:title \"My API\" :version \"1.0.0\"}
       {\"/users\" {:get {...} :post {...}}}
       {:User {...}}
       {:servers [{:url \"http://localhost:3000\"}]
        :tags [{:name \"Users\"}]})
   
   Args:
     info - Map with :title, :version, :description (optional)
     paths - Map of path definitions
     schemas - Map of schema definitions
     options - Map with :servers, :tags (optional)
   
   Returns:
     Complete OpenAPI specification map"
  [info paths schemas & [{:keys [servers tags] :as opts}]]
  
  (cond->
    {:openapi "3.1.1"
     :info (select-keys info [:title :version :description])
     :paths paths
     :components {:schemas schemas}}
    
    servers (assoc :servers servers)
    tags (assoc :tags tags)))
