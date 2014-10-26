(ns routr.web
  (:use compojure.core
        [ring.adapter.jetty :only [run-jetty]]
        hiccup.core
        hiccup.page
        [hiccup.middleware :only (wrap-base-url)])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.pprint :as pprint]
            [ring.util.response :as response]
            [clojure.java.jdbc :as sql]
            [hiccup.element :as element]
            [clj-http.client :as client])
  (:import [org.lesscss LessCompiler]
           [java.io File]
           [java.sql Timestamp])
  (:gen-class))

(defn- file [filename]
  (new File filename))

(defn- include-less [location filename]
  (-> (new LessCompiler)
    (.compile (file (str "resources/public" location "/" filename ".less")) (file (str "resources/public/css/compiled" location "/" filename ".css")) false))
  (include-css (str "/css/compiled" location "/" filename ".css")))

(def database (System/getenv "DATABASE_URL"))

(defn- record [point]
  (sql/insert! database :locations [:time :spot] [(new Timestamp (System/currentTimeMillis)) point]))

(defn- read-row [row]
  (let [time (:time row)
              lat (subs (:spot row) 4 13)
              lng (subs (:spot row) 18)]
          {:time time
           :lat lat ; lat-46.248633;long6.663343
           :lng lng}
          ))

(defn- query-points [limit]
  (sql/query database [(str "select time, spot from locations order by time desc limit " limit)]
    :row-fn read-row))

(defn- last-point []
  (first (query-points 1)))

(defn- trail []
  (query-points 8))

(defn- write-lines [& lines]
  (apply str (map println-str lines)))

(defn- to-lat-long-json [lat-long]
  (str "{ lat: " (:lat lat-long) ", lng: " (:lng lat-long) " }"))

(defroutes the-routes
  (GET "/whereiam" [point]
    (record point)
    (html5
      [:body
       [:p (str "Point" point " recorded to " (System/getenv "DATABASE_URL") "<")]
      ]))
  (GET "/where" []
    (html5
      [:head
        [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, user-scalable=no"}]
        [:style {:type "text/css"}
          (write-lines 
            "html { height: 100% }"
            "body { height: 100%; margin: 0; padding: 0 }"
            "#map-canvas { height: 100% }")]]
      (include-js "/javascript/jquery/jquery-1.9.1.js")
      (include-js "https://maps.googleapis.com/maps/api/js?key=AIzaSyDscr8WySSq2eoVyJ6bBbOqTNEMawh3WAA&sensor=false")
      (element/javascript-tag
        (let [latest-point (last-point)]
          (write-lines
          (str "var routrMapCenter = { lat: " (:lat latest-point) ", lng: " (:lng latest-point) " };")
          (str "var routrLastTime = \"" (:time latest-point)  "\";")
          (str "var routrTrail = [ " (clojure.string/join "," (map to-lat-long-json (trail))) " ];"))))
      (include-js "/javascript/routr/where.js")
      [:body
       [:div {:id "map-canvas"}]]))
  (GET "/" []
    (html5 
      [:head 
       [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
       (include-less "/javascript/twitter-bootstrap/less" "bootstrap")
       (include-less "/javascript/twitter-bootstrap/less" "responsive")
       (include-js 
         "/javascript/jquery/jquery-1.9.1.js" 
         "/javascript/jquery-ui/jquery-ui-1.10.2/ui/jquery-ui.js" 
         "/javascript/routr/fit-banner.js")
       (include-css "/css/root.css")
       [:title "Locadr"]]
      [:body
       [:table {:style "width:100%; height:100%;"}
        [:tr 
         [:td {:style "vertical-align: middle; text-align: center;"} 
          [:span {:style "position: relative;" :class "fit-banner banner-text hover-highlight"} "Sienna Davies"
           [:span {:style "position: absolute; top: 100%; right: 0px; font-size:14px;"}
            [:table
             [:tr [:td [:input {:class "login" :type "text" :placeholder "Username"} ]]]
             [:tr [:td [:input {:class "login" :type "password" :placeholder "Password"} ]]]
            ]
           ]
          ]
         ] 
        ]
       ]
      ]))
  (GET "/signin" []
    (println "redirecting...")
    (response/redirect
      (str
        "https://accounts.google.com/o/oauth2/auth?"
        "client_id=" "659416221395-vlkflcmp31r5ie9s1vi2t8igffhhrvqi.apps.googleusercontent.com"
        "&response_type=code"
        "&scope=openid%20email"
        "&redirect_uri=http://localhost:9000/oauth2callback"
        "&state=1"
;        "&access_type=offline"
;        "&prompt=select_account"
        )))
  (GET "/oauth2callback" [code]
    (println "callback:" code)
    (let [response (client/post "https://accounts.google.com/o/oauth2/token"
                           {:socket-timeout 1000  ;; in milliseconds
                            :conn-timeout 1000    ;; in milliseconds
                            :as :json
                            :accept :json
                            :form-params{:code code
                                         :client_id "659416221395-vlkflcmp31r5ie9s1vi2t8igffhhrvqi.apps.googleusercontent.com"
                                         :client_secret (System/getenv "GOOGLE_OAUTH2_CLIENT_SECRET")
                                         :redirect_uri "http://localhost:9000/oauth2callback"
                                         :grant_type "authorization_code"}})]
      (println "in oauth2 callback")
      (println (:body response))
      (let [access_token (:access_token (:body response))
            _ (println (str "access token: " access_token))
            basicinfo (client/get (str "https://www.googleapis.com/oauth2/v1/userinfo?access_token=" access_token))]
        (println "basic info")
        (println basicinfo)
        (html5 
          [:body
           [:p (str basicinfo)]])
        )))
  
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>"))

(defn- protocol [request]
  (get (:headers request) "x-forwarded-proto"))

(defn- redirect-https? [request]
  (not (or (= "development" (System/getProperty "routr.environment"))
           (= "https" (protocol request)))))

(defn redirect-non-https [handler]
  (fn [request]
    (if (redirect-https? request)
      (response/redirect (str "https://" (get (:headers request) "host") (:uri request)))
      (handler request))))

(def app 
  (-> (handler/site the-routes)
      (redirect-non-https)
      (wrap-base-url)))

(defn -main [& args]
  (run-jetty #'app {:port (Integer. (first args))}))
