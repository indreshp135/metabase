(ns metabase.integrations.sftpgo
  "SFTPGo integration."
  (:require
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [metabase.models.setting :as setting :refer [defsetting]]
   [metabase.util.i18n :refer [tru deferred-tru]]))

;; SFTPGo has only username and password and no client id or secret. It is enabled if username and password are set and url.
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defsetting sftpgo-auth-username
  (deferred-tru "Username for SFTPGo Sign-In.")
  :visibility :public
  :setter     (fn [username]
                (if (seq username)
                  (let [trimmed-username (str/trim username)]
                    (setting/set-value-of-type! :string :sftpgo-auth-username trimmed-username))
                  (do
                    (setting/set-value-of-type! :string :sftpgo-auth-username nil)
                    (setting/set-value-of-type! :boolean :sftpgo-auth-enabled false)))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defsetting sftpgo-auth-password
  (deferred-tru "Password for SFTPGo Sign-In.")
  :visibility :public
  :setter     (fn [password]
                (if (seq password)
                  (let [trimmed-password (str/trim password)]
                    (setting/set-value-of-type! :string :sftpgo-auth-password trimmed-password))
                  (do
                    (setting/set-value-of-type! :string :sftpgo-auth-password nil)
                    (setting/set-value-of-type! :boolean :sftpgo-auth-enabled false)))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defsetting sftpgo-auth-url
  (deferred-tru "URL for SFTPGo Sign-In.")
  :visibility :public
  :setter     (fn [url]
                (if (seq url)
                  (let [trimmed-url (str/trim url)]
                    (setting/set-value-of-type! :string :sftpgo-auth-url trimmed-url))
                  (do
                    (setting/set-value-of-type! :string :sftpgo-auth-url nil)
                    (setting/set-value-of-type! :boolean :sftpgo-auth-enabled false)))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defsetting sftpgo-auth-enabled
  (deferred-tru "Enable SFTPGo Sign-In.")
  :visibility :public
  :type       :boolean
  :getter     (fn []
                (if (and (setting/get :sftpgo-auth-username)
                         (setting/get :sftpgo-auth-password)
                         (setting/get :sftpgo-auth-url))
                  true
                  false))
  :setter     (fn [enabled]
                (if enabled
                  (if (and (setting/get :sftpgo-auth-username)
                           (setting/get :sftpgo-auth-password)
                           (setting/get :sftpgo-auth-url))
                    (setting/set-value-of-type! :boolean :sftpgo-auth-enabled true)
                    (do
                      (setting/set-value-of-type! :boolean :sftpgo-auth-enabled false)
                      (throw (Exception. (tru "SFTPGo is not configured.")))))
                  (setting/set-value-of-type! :boolean :sftpgo-auth-enabled false))))

(defn- encode [to-encode]
  (.encodeToString (java.util.Base64/getEncoder) (.getBytes to-encode "UTF-8")))

;; Function to check if SFTPGo is configured
(defn sftpgo-configured?
  "Check if SFTPGo is configured."
  []
  (setting/get :sftpgo-auth-enabled))

;; First, define a function to get the access token
(defn- get-access-token [username password url]
  (let [credentials (str username ":" password)
        encoded-credentials (encode credentials)
        basic-auth (str "Basic " encoded-credentials)
        headers {"Authorization" basic-auth}
        {:keys [body]} (http/get (str url "/api/v2/user/token")
                                 {:headers headers})]
    (get (json/parse-string body) "access_token")))


;; Then, define a function to upload a file
(defn- upload-file
  "Upload a file to SFTPGo through the API."
  [access-token file url file-name subscription-name subscription_folder_path date]
  (let [headers {"Authorization" (str "Bearer " access-token)}
        response (http/post (str url "/api/v2/user/files/upload")
                            {:headers headers
                             :query-params
                             {:path
                              (if (= date "")
                                (str subscription_folder_path "/" subscription-name "/" file-name)
                                (str subscription_folder_path "/" subscription-name "_" (str date) "/" file-name)) :mkdir_parents true}
                             :body file})]
    (json/parse-string (:body response))))

(defn- upload-file-to-sftpgo
  "Upload a file to SFTPGo."
  [file file-name subscription-name subscription_folder_path date]
  (let [username (setting/get :sftpgo-auth-username)
        password (setting/get :sftpgo-auth-password)
        url (setting/get :sftpgo-auth-url)
        access-token (get-access-token username password url)]
    (upload-file access-token file url file-name subscription-name subscription_folder_path date)))

(defn string-to-html-file
  "Convert a string to a html file."
  [string]
  (let [file (java.io.File/createTempFile "temp" ".html")]
    (spit file string)
    file))

(defn send-file-or-throw!
  "Send a file to SFTPGo and return the URL to the file."
  [files subscription-name subject subscription_folder_path date]
  ;; For each file in files, upload to SFTPGo 
  (doseq [file files]
    ;; Initialize date variable 
    (let
     [type (:type file)
      file-name (if (= (:type file) "text/html; charset=utf-8")
                  (if (= date "")
                    (str subject ".html")
                    (str subject "_" (str date)  ".html"))
                  ;; Check if file-name is nil 
                  (if (not (:file-name file))
                    (str (:content-id file))
                    (str (:file-name file))))
      file-buffer (if (= type "text/html; charset=utf-8")
                    (string-to-html-file (:content file))
                    ;;(io/input-stream (str (:content file)))
                    (io/file (:content file)))]
      (println "Uploading file: " file-buffer "with type" type "and name" file-name)
      (upload-file-to-sftpgo file-buffer file-name subscription-name subscription_folder_path date))))


(defn get-folder-tree
  "Get the folder tree from SFTPGo."
  [path]
  (let [url (setting/get :sftpgo-auth-url)
        username (setting/get :sftpgo-auth-username)
        password (setting/get :sftpgo-auth-password)
        access-token (get-access-token username password url)
        headers {"Authorization" (str "Bearer " access-token)}
        response (http/get (str url "/api/v2/user/dirs")
                           {:headers headers
                            :query-params {:path path}})
        body (json/parse-string (:body response))]
      (map (fn [item]
             (if (nil? (get item "size"))
               (assoc item :isFolder true :items (get-folder-tree (get item "name")))
               (assoc item :isFolder false :items [])))
           body)))
      
