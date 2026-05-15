# OpenAPI Library for Ring/Compojure

A reusable, library-friendly OpenAPI 3.1.1 + Swagger UI integration for Ring/Compojure-based Clojure applications.

## Features

- ✅ OpenAPI 3.1.1 specification support
- ✅ Declarative API schema and path definitions
- ✅ Automatic Swagger UI generation and hosting
- ✅ Composable middleware design
- ✅ Zero boilerplate response handling
- ✅ Easy integration into existing Ring/Compojure apps

## Installation

Add to your `project.clj`:

```clojure
:dependencies [[simple-openapi "0.1.0"]]
```

Or use `deps.edn`:

```clojure
{:deps {simple-openapi {:mvn/version "0.1.0"}}}
```

## Quick Start

### 1. Define Your Schemas

```clojure
(require '[openapi.spec :refer [object-schema string-schema integer-schema]])

(def schemas
  {:User
   (object-schema
     {:id (integer-schema)
      :name (string-schema)
      :email (string-schema :format "email")}
     :required ["id" "name" "email"])})
```

### 2. Define Your API Paths

```clojure
(require '[openapi.spec :refer [get-op post-op response array-schema]])

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
       :request-schema {"$ref" "#/components/schemas/User"}
       :responses
       {"201" (response "User created"
                :schema {"$ref" "#/components/schemas/User"})}})}})
```

### 3. Create the OpenAPI Spec

```clojure
(require '[openapi.spec :refer [openapi-spec]])

(def api-spec
  (openapi-spec
    {:title "My API" :version "1.0.0"}
    paths
    schemas
    {:servers [{:url "http://localhost:3000"}]}))
```

### 4. Add Swagger UI Middleware

```clojure
(require '[openapi.lib :refer [wrap-swagger-ui]])
(require '[ring.middleware.json :refer [wrap-json-body wrap-json-response]])

(def app
  (-> routes
      (wrap-swagger-ui {:openapi-spec api-spec
                        :swagger-ui-path "/docs"
                        :openapi-json-path "/api/openapi.json"})
      (wrap-json-body {:keywords? true})
      wrap-json-response))
```

## API Reference

### Response Helpers

```clojure
(require '[openapi.lib :refer [success-response created-response error-response]])

; Create 200 OK responses
(success-response {:status "ok"})

; Create 201 Created responses
(created-response new-resource)

; Create error responses with custom status
(error-response 404 "Resource not found")
```

### Schema Builders

#### Basic Types

```clojure
(require '[openapi.spec :refer [string-schema integer-schema number-schema boolean-schema]])

(string-schema :min-length 1 :max-length 255)
(integer-schema :minimum 0 :maximum 100)
(number-schema)
(boolean-schema)
```

#### Complex Types

```clojure
(require '[openapi.spec :refer [object-schema array-schema]])

; Object schema with properties and required fields
(object-schema
  {:name (string-schema)
   :age (integer-schema)}
  :required ["name"]
  :description "User object")

; Array schema
(array-schema (string-schema) :min-items 1)
```

### Parameter Builders

```clojure
(require '[openapi.spec :refer [path-parameter query-parameter header-parameter]])

(path-parameter "id" (integer-schema) :description "User ID")
(query-parameter "limit" (integer-schema) :description "Page limit")
(header-parameter "X-Token" (string-schema) :required true)
```

### Operation Builders

```clojure
(require '[openapi.spec :refer [get-op post-op put-op patch-op delete-op]])

(get-op "Get all users"
  {:tags ["Users"]
   :parameters [...]
   :responses {...}})

(post-op "Create user"
  {:request-schema ...
   :responses {...}})
```

### Response Builder

```clojure
(require '[openapi.spec :refer [response]])

(response "Success message"
  :schema {:type "object" :properties {...}}
  :headers {...})
```

## Example: Complete API

See `src/openapi/demo/core.clj` for a complete working example with:
- Health check endpoint
- User listing
- User creation with validation
- User lookup by ID

To run the demo:

```bash
lein run
```

Then visit:
- Swagger UI: http://localhost:3000/swagger-ui
- OpenAPI JSON: http://localhost:3000/openapi.json

## Integration Guide

To integrate into an existing Ring/Compojure application:

### Step 1: Define Your Schemas

Create a new namespace for your API schemas:

```clojure
(ns myapp.api.schemas
  (:require [openapi.spec :refer [...]]))

(def user-schema ...)
(def error-schema ...)
```

### Step 2: Define Your Paths

Create a namespace for your API documentation:

```clojure
(ns myapp.api.docs
  (:require [openapi.spec :refer [...]]))

(def paths {"/users" {...}})
```

### Step 3: Compose Your App

```clojure
(ns myapp.core
  (:require 
    [openapi.lib :refer [wrap-swagger-ui]]
    [openapi.spec :refer [openapi-spec]]
    [myapp.api.schemas :as schemas]
    [myapp.api.docs :as docs]))

(def api-spec
  (openapi-spec
    {:title "My App" :version "1.0.0"}
    docs/paths
    schemas/schemas))

(def app
  (-> routes
      (wrap-swagger-ui {:openapi-spec api-spec
                        :swagger-ui-path "/api/docs"
                        :openapi-json-path "/api/spec.json"})
      middleware-chain...))
```

## Library Structure

```
src/
├── openapi/
│   ├── lib.clj           # Core library utilities
│   ├── spec.clj          # OpenAPI spec builders
│   └── demo/
│       └── core.clj      # Demo application
```

### `openapi.lib`

Core utilities for:
- Response helpers (success, error, created)
- Swagger UI HTML generation
- OpenAPI JSON endpoint middleware

### `openapi.spec`

Declarative builders for:
- Schemas (object, array, string, number, etc.)
- Parameters (path, query, header)
- Operations (GET, POST, PUT, PATCH, DELETE)
- Responses
- Complete OpenAPI specifications

## Customization

### Custom Swagger UI Path

```clojure
(wrap-swagger-ui {:openapi-spec api-spec
                  :swagger-ui-path "/custom/docs"
                  :openapi-json-path "/custom/spec.json"})
```

### Adding Security Schemes

Modify the spec after creating it:

```clojure
(def api-spec
  (-> (openapi-spec {...})
      (assoc-in [:components :securitySchemes]
                {:bearerAuth {...}})))
```

### Server-Relative Paths

```clojure
(def api-spec
  (openapi-spec
    {:title "API" :version "1.0.0"}
    paths
    schemas
    {:servers [{:url "/api/v1" :description "API Server"}]}))
```

## Performance Considerations

- OpenAPI spec is generated once and served as static JSON
- Swagger UI is CDN-hosted (unpkg.com) for minimal bundle size
- No runtime overhead for your API handlers

## Troubleshooting

### Swagger UI not showing

Ensure:
1. The middleware is applied **before** JSON middleware
2. You're accessing the correct path (default: `/swagger-ui`)
3. Browser can reach unpkg.com for Swagger UI assets

### OpenAPI JSON returns 404

Ensure:
1. The middleware is in the chain
2. You're accessing the correct JSON endpoint (default: `/openapi.json`)

### Schema references not resolving

Ensure:
1. Schema names match the `$ref` values exactly
2. Format: `{"$ref" "#/components/schemas/SchemaName"}`

## License

Apache License 2.0 - See LICENSE file

## Contributing

Contributions welcome! Please ensure:
- Tests pass
- Code is well-documented
- Examples are updated
