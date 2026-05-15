# OpenAPI Library Usage Guide

This guide provides detailed instructions for using the OpenAPI library in your Ring/Compojure Clojure projects.

## Table of Contents

1. [Installation](#installation)
2. [Core Concepts](#core-concepts)
3. [Schema Definition](#schema-definition)
4. [Path Definition](#path-definition)
5. [Complete Example](#complete-example)
6. [Best Practices](#best-practices)
7. [Advanced Usage](#advanced-usage)

## Installation

### Via project.clj

```clojure
(defproject myapp "0.1.0"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring/ring-core "1.12.1"]
                 [ring/ring-json "0.5.1"]
                 [compojure "1.7.1"]
                 [simple-openapi "0.1.0"]])
```

### Via deps.edn

```clojure
{:deps {org.clojure/clojure {:mvn/version "1.11.1"}
        ring/ring-core {:mvn/version "1.12.1"}
        ring/ring-json {:mvn/version "0.5.1"}
        compojure {:mvn/version "1.7.1"}
        simple-openapi {:mvn/version "0.1.0"}}}
```

## Core Concepts

The library separates concerns into three main areas:

### 1. Response Helpers (`openapi.lib`)

Provides composable functions for creating HTTP responses:

```clojure
(success-response data)       ; 200 OK
(created-response data)       ; 201 Created
(error-response status msg)   ; Error with custom status
```

### 2. Spec Builders (`openapi.spec`)

Provides declarative DSL for building OpenAPI schemas and paths:

```clojure
(string-schema)               ; String type
(object-schema props)         ; Object type
(get-op summary opts)         ; GET operation
(response description)        ; Response definition
```

### 3. Middleware (`openapi.lib`)

Automatically serves Swagger UI and OpenAPI JSON:

```clojure
(wrap-swagger-ui handler opts)  ; Add Swagger UI endpoints
```

## Schema Definition

### Scalars

```clojure
(require '[openapi.spec :refer [string-schema integer-schema number-schema boolean-schema]])

; String with constraints
(string-schema :min-length 1 :max-length 255 :pattern "^[a-z]+$")

; String with format
(string-schema :format "email")
(string-schema :format "date-time")

; String with enum
(string-schema :enum ["active" "inactive" "pending"])

; Integer with constraints
(integer-schema :minimum 0 :maximum 100)

; Number (float/decimal)
(number-schema :minimum 0.0)

; Boolean
(boolean-schema)
```

### Objects

```clojure
(require '[openapi.spec :refer [object-schema string-schema integer-schema]])

; Simple object
(object-schema
  {:name (string-schema)
   :age (integer-schema)})

; With required fields
(object-schema
  {:name (string-schema)
   :email (string-schema :format "email")
   :age (integer-schema)}
  :required ["name" "email"])

; With description
(object-schema
  {:id (integer-schema)
   :created-at (string-schema :format "date-time")}
  :required ["id"]
  :description "Audit fields for all resources")

; Nested objects
(object-schema
  {:user (object-schema
           {:id (integer-schema)
            :name (string-schema)})
   :count (integer-schema)})
```

### Arrays

```clojure
(require '[openapi.spec :refer [array-schema string-schema]])

; Array of strings
(array-schema (string-schema))

; Array of objects with constraints
(array-schema
  (object-schema {:id (integer-schema) :name (string-schema)})
  :min-items 1
  :max-items 100)
```

### Schema Reuse

Define common schemas once and reuse via `$ref`:

```clojure
(def schemas
  {:User
   (object-schema
     {:id (integer-schema)
      :name (string-schema)
      :email (string-schema :format "email")}
     :required ["id" "name" "email"])

   :Error
   (object-schema
     {:code (string-schema)
      :message (string-schema)}
     :required ["code" "message"])

   :PaginatedUsers
   (object-schema
     {:data (array-schema {"$ref" "#/components/schemas/User"})
      :total (integer-schema)
      :page (integer-schema)})})
```

## Path Definition

### Basic GET Operation

```clojure
(require '[openapi.spec :refer [get-op response]])

(def paths
  {"/health"
   {:get
    (get-op "Health Check"
      {:tags ["System"]
       :responses
       {"200" (response "Service is healthy"
                :schema {:type "object"
                         :properties {:status {:type "string"}}})}})}})
```

### GET with Parameters

```clojure
(require '[openapi.spec :refer [get-op path-parameter query-parameter response]])

(def paths
  {"/users/{id}"
   {:get
    (get-op "Get user by ID"
      {:tags ["Users"]
       :parameters
       [(path-parameter "id" (integer-schema) :description "User ID")
        (query-parameter "include" (string-schema) :description "Related fields")]
       :responses
       {"200" (response "User found"
                :schema {"$ref" "#/components/schemas/User"})
        "404" (response "User not found"
                :schema {"$ref" "#/components/schemas/Error"})}})}})
```

### POST with Request Body

```clojure
(require '[openapi.spec :refer [post-op response]])

(def paths
  {"/users"
   {:post
    (post-op "Create user"
      {:tags ["Users"]
       :request-schema {"$ref" "#/components/schemas/CreateUserRequest"}
       :responses
       {"201" (response "User created"
                :schema {"$ref" "#/components/schemas/User"})
        "400" (response "Invalid request"
                :schema {"$ref" "#/components/schemas/Error"})}})}})
```

### Multiple Methods on Same Path

```clojure
(require '[openapi.spec :refer [get-op post-op put-op delete-op]])

(def paths
  {"/users/{id}"
   {:get    (get-op "Get user" {...})
    :put    (put-op "Update user" {...})
    :delete (delete-op "Delete user" {...})}})
```

## Complete Example

### Step 1: Define Schemas

File: `src/myapp/api/schemas.clj`

```clojure
(ns myapp.api.schemas
  (:require [openapi.spec :refer [object-schema string-schema integer-schema]]))

(def schemas
  {:User
   (object-schema
     {:id (integer-schema)
      :name (string-schema :min-length 1)
      :email (string-schema :format "email")}
     :required ["id" "name" "email"]
     :description "User object")

   :CreateUserRequest
   (object-schema
     {:name (string-schema :min-length 1)
      :email (string-schema :format "email")}
     :required ["name" "email"]
     :description "Request to create a user")

   :ErrorResponse
   (object-schema
     {:error (string-schema)}
     :required ["error"]
     :description "Error response")})
```

### Step 2: Define Paths

File: `src/myapp/api/paths.clj`

```clojure
(ns myapp.api.paths
  (:require [openapi.spec :refer [get-op post-op response array-schema
                                   path-parameter]]))

(def paths
  {"/users"
   {:get
    (get-op "List all users"
      {:tags ["Users"]
       :responses
       {"200" (response "List of users"
                :schema (array-schema {"$ref" "#/components/schemas/User"}))}})

    :post
    (post-op "Create a user"
      {:tags ["Users"]
       :request-schema {"$ref" "#/components/schemas/CreateUserRequest"}
       :responses
       {"201" (response "User created"
                :schema {"$ref" "#/components/schemas/User"})
        "400" (response "Invalid request"
                :schema {"$ref" "#/components/schemas/ErrorResponse"})}})}

   "/users/{id}"
   {:get
    (get-op "Get user by ID"
      {:tags ["Users"]
       :parameters
       [(path-parameter "id" (integer-schema) :description "User ID")]
       :responses
       {"200" (response "User found"
                :schema {"$ref" "#/components/schemas/User"})
        "404" (response "User not found"
                :schema {"$ref" "#/components/schemas/ErrorResponse"})}})}})
```

### Step 3: Define Handlers

File: `src/myapp/api/handlers.clj`

```clojure
(ns myapp.api.handlers
  (:require [openapi.lib :refer [success-response created-response error-response]]))

(def users (atom [{:id 1 :name "John" :email "john@example.com"}]))

(defn list-users [_]
  (success-response @users))

(defn create-user [{:keys [body]}]
  (let [{:keys [name email]} body]
    (cond
      (nil? name) (error-response 400 "name is required")
      (nil? email) (error-response 400 "email is required")
      :else
      (let [user {:id (inc (count @users)) :name name :email email}]
        (swap! users conj user)
        (created-response user)))))

(defn get-user [{:keys [params]}]
  (let [user-id (Integer/parseInt (:id params))
        user (first (filter #(= (:id %) user-id) @users))]
    (if user
      (success-response user)
      (error-response 404 "User not found"))))
```

### Step 4: Build Routes

File: `src/myapp/api/routes.clj`

```clojure
(ns myapp.api.routes
  (:require
    [compojure.core :refer [GET POST defroutes]]
    [compojure.route :as route]
    [openapi.lib :refer [error-response]]
    [myapp.api.handlers :as handlers]))

(defroutes api-routes
  (GET "/users" req (handlers/list-users req))
  (POST "/users" req (handlers/create-user req))
  (GET "/users/:id" req (handlers/get-user req))
  (route/not-found (error-response 404 "Route not found")))
```

### Step 5: Create Application

File: `src/myapp/core.clj`

```clojure
(ns myapp.core
  (:require
    [ring.adapter.jetty :refer [run-jetty]]
    [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
    [openapi.lib :refer [wrap-swagger-ui]]
    [openapi.spec :refer [openapi-spec]]
    [myapp.api.routes :refer [api-routes]]
    [myapp.api.schemas :as schemas]
    [myapp.api.paths :as paths]))

(def api-spec
  (openapi-spec
    {:title "My User API"
     :version "1.0.0"
     :description "Simple user management API"}
    paths/paths
    schemas/schemas
    {:servers [{:url "http://localhost:3000" :description "Development"}]
     :tags [{:name "Users" :description "User management"}]}))

(def app
  (-> api-routes
      (wrap-swagger-ui {:openapi-spec api-spec
                        :swagger-ui-path "/docs"
                        :openapi-json-path "/api.json"})
      (wrap-json-body {:keywords? true})
      wrap-json-response))

(defn start-server []
  (run-jetty app {:port 3000 :join? false}))

(defn -main [& _]
  (start-server))
```

## Best Practices

### 1. Organize by Concern

```
src/
├── myapp/
│   ├── api/
│   │   ├── handlers.clj    # HTTP handlers
│   │   ├── schemas.clj     # OpenAPI schemas
│   │   ├── paths.clj       # OpenAPI paths
│   │   └── routes.clj      # Compojure routes
│   └── core.clj            # App setup
```

### 2. Reuse Schemas

Define schemas once, reference via `$ref`:

```clojure
:responses
{"200" (response "User" :schema {"$ref" "#/components/schemas/User"})}
```

### 3. Document Descriptions

Always include descriptions in schemas and operations:

```clojure
(object-schema {...} :description "Important resource")
(get-op "Clear description" {...})
```

### 4. Use Proper HTTP Status Codes

```clojure
; Use correct status codes
{"200" (response "Success")      ; OK
 "201" (response "Created")      ; Created
 "204" (response "No content")   ; No content (for DELETE)
 "400" (response "Bad request")  ; Validation error
 "404" (response "Not found")    ; Resource not found
 "500" (response "Server error")}; Internal error
```

### 5. Middleware Order

Always apply OpenAPI middleware **before** JSON middleware:

```clojure
(-> routes
    (wrap-swagger-ui {...})          ; First
    (wrap-json-body {...})           ; Second
    wrap-json-response)              ; Third
```

## Advanced Usage

### Custom Response Headers

```clojure
(response "Success"
  :headers {"X-Total-Count" {:schema {:type "integer"}}})
```

### Multiple Servers

```clojure
(openapi-spec
  config
  paths
  schemas
  {:servers [{:url "http://localhost:3000" :description "Development"}
             {:url "http://api.example.com" :description "Production"}]})
```

### Security Schemes

```clojure
(def api-spec
  (-> (openapi-spec config paths schemas)
      (assoc-in [:components :securitySchemes]
                {:bearerAuth {:type "http"
                             :scheme "bearer"
                             :bearerFormat "JWT"}})))
```

### Tags for Organization

```clojure
(openapi-spec
  config
  paths
  schemas
  {:tags [{:name "Users" :description "User management"}
          {:name "Auth" :description "Authentication"}
          {:name "System" :description "System status"}]})
```

## Troubleshooting

### Issue: Schemas not found in Swagger UI

**Solution:** Ensure schema names match exactly in `$ref`:
```clojure
; Define schema
:User {...}

; Reference must match
{"$ref" "#/components/schemas/User"}
```

### Issue: Custom paths not showing in Swagger UI

**Solution:** Check middleware ordering - `wrap-swagger-ui` must come before JSON middleware

### Issue: Swagger UI CSS not loading

**Solution:** Ensure browser can reach `https://unpkg.com`. Check firewall/proxy settings.

## Next Steps

- Review the [README.md](README.md) for API reference
- Check [demo application](src/openapi/demo/core.clj) for working example
- Explore `openapi.lib` and `openapi.spec` namespaces for full API
