(ns openapi.demo.core
  (:require
   [compojure.core :refer [GET POST defroutes]]
   [compojure.route :as route]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
   [openapi.lib :refer [success-response created-response error-response
                        wrap-swagger-ui]]
   [openapi.spec :refer [string-schema integer-schema array-schema
                         object-schema path-parameter
                         response openapi-spec get-op post-op]]))

;; =============================================================================
;; CONFIGURATION
;; =============================================================================

(def server-config
  {:port 3000})

(def openapi-config
  {:title "Simple User API"
   :version "1.0.0"
   :description "Minimal Ring + OpenAPI 3.1.1 Example"})

;; =============================================================================
;; IN-MEMORY DATABASE
;; =============================================================================

(def users
  (atom
   [{:id 1
     :name "Anuj Kumar"
     :email "anuj@example.com"}

    {:id 2
     :name "John Doe"
     :email "john@example.com"}]))

;; =============================================================================
;; OPENAPI SCHEMAS
;; =============================================================================
;;
;; Using openapi.spec helpers to define schemas declaratively
;;

(def api-schemas
  {:HealthResponse
   (object-schema
     {:status (string-schema :description "Service status")}
     :required ["status"]
     :description "Health check response")

   :User
   (object-schema
     {:id (integer-schema :description "User ID")
      :name (string-schema :description "User name")
      :email (string-schema :format "email" :description "User email")}
     :required ["id" "name" "email"]
     :description "User object")

   :CreateUserRequest
   (object-schema
     {:name (string-schema :min-length 2 :description "User name")
      :email (string-schema :format "email" :description "User email")}
     :required ["name" "email"]
     :description "Request to create a new user")

   :ErrorResponse
   (object-schema
     {:error (string-schema :description "Error message")}
     :required ["error"]
     :description "Error response")})

;; =============================================================================
;; OPENAPI PATH DEFINITIONS
;; =============================================================================
;;
;; Using openapi.spec helpers to define API paths declaratively
;;

(def api-paths
  {"/health"
   {:get
    (get-op "Health Check"
      {:tags ["Health"]
       :responses
       {"200" (response "Service is healthy"
                :schema {"$ref" "#/components/schemas/HealthResponse"})}})}

   "/users"
   {:get
    (get-op "Get All Users"
      {:tags ["Users"]
       :responses
       {"200" (response "List of users"
                :schema (array-schema {"$ref" "#/components/schemas/User"}))}})

    :post
    (post-op "Create User"
      {:tags ["Users"]
       :request-schema {"$ref" "#/components/schemas/CreateUserRequest"}
       :responses
       {"201" (response "User created"
                :schema {"$ref" "#/components/schemas/User"})
        "400" (response "Invalid request"
                :schema {"$ref" "#/components/schemas/ErrorResponse"})}})}

   "/users/{id}"
   {:get
    (get-op "Get User By ID"
      {:tags ["Users"]
       :parameters [(path-parameter "id" (integer-schema)
                     :description "User ID")]
       :responses
       {"200" (response "User found"
                :schema {"$ref" "#/components/schemas/User"})
        "404" (response "User not found"
                :schema {"$ref" "#/components/schemas/ErrorResponse"})}})}})

;; =============================================================================
;; OPENAPI DOCUMENT BUILDER
;; =============================================================================

(def openapi-spec-doc
  (openapi-spec
    openapi-config
    api-paths
    api-schemas
    {:servers [{:url (str "http://localhost:" (:port server-config))
               :description "Local Development"}]
     :tags [{:name "Health" :description "Health APIs"}
            {:name "Users" :description "User APIs"}]}))

;; =============================================================================
;; BUSINESS LOGIC
;; =============================================================================

(defn next-user-id
  []
  (inc
   (apply max
          (cons 0 (map :id @users)))))

(defn find-user
  [user-id]
  (first
   (filter #(= (:id %) user-id)
           @users)))

(defn create-user!
  [{:keys [name email]}]

  (let [user {:id (next-user-id)
              :name name
              :email email}]

    (swap! users conj user)

    user))

;; =============================================================================
;; API HANDLERS
;; =============================================================================

(defn health-handler
  [_]
  (success-response
   {:status "UP"}))

(defn get-users-handler
  [_]
  (success-response @users))

(defn create-user-handler
  [req]

  (let [{:keys [name email]} (:body req)]

    (cond

      (nil? name)
      (error-response 400 "name is required")

      (nil? email)
      (error-response 400 "email is required")

      :else
      (created-response
       (create-user! {:name name
                      :email email})))))

(defn get-user-handler
  [id]

  (let [user-id (Integer/parseInt id)
        user (find-user user-id)]

    (if user
      (success-response user)
      (error-response 404 "User not found"))))

;; =============================================================================
;; ROUTES
;; =============================================================================

(defroutes app-routes
  ;; API Routes
  (GET "/health" req
    (health-handler req))

  (GET "/users" req
    (get-users-handler req))

  (POST "/users" req
    (create-user-handler req))

  (GET "/users/:id" [id]
    (get-user-handler id))

  ;; 404
  (route/not-found
   (error-response 404 "Route not found")))

;; =============================================================================
;; APPLICATION
;; =============================================================================

(def app
  "Main Ring application with middleware chain and Swagger UI"
  (-> app-routes
      (wrap-swagger-ui {:openapi-spec openapi-spec-doc
                        :swagger-ui-path "/swagger-ui"
                        :openapi-json-path "/openapi.json"})
      (wrap-json-body {:keywords? true})
      wrap-json-response))

;; =============================================================================
;; SERVER
;; =============================================================================

(defn start-server
  "Start the Jetty server with the configured port"
  []
  (println "-----------------------------------")
  (println "Server running")
  (println)
  (println "Swagger UI:")
  (println (str "http://localhost:" (:port server-config) "/swagger-ui"))
  (println)
  (println "OpenAPI JSON:")
  (println (str "http://localhost:" (:port server-config) "/openapi.json"))
  (println "-----------------------------------")

  (run-jetty app {:port (:port server-config) :join? false}))

(defn -main [& _]
  "Entry point for the application"
  (start-server))
