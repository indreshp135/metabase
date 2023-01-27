(ns metabase.api.sftpgo
  "/api/sftpgo endpoints"
  (:require
   [compojure.core :refer [PUT]]
   [metabase.api.common :as api]
   [metabase.integrations.sftpgo :as sftpgo]
   [metabase.models.setting :as setting]
   [schema.core :as s]
   [toucan.db :as db]))

#_{:clj-kondo/ignore [:deprecated-var]}
(api/defendpoint-schema PUT "/settings"
  "Update SFTPGo related settings. You must be a superuser or have `setting` permission to do this."
  [:as {{:keys [sftpgo-auth-connections]} :body}]
  (println "sftpgo-auth-connections" sftpgo-auth-connections)
  {sftpgo-auth-connections                (s/maybe [s/Str])}
  (db/transaction
   (let [valid-connections (filter #(and (:url %) (:username %) (:password %) (:name %)) sftpgo-auth-connections)]
     (setting/set-many! {:sftpgo-auth-connections                valid-connections
                         :sftpgo-auth-enabled                    (not-empty valid-connections)})
     (sftpgo/sftpgo-auth-enabled! (not-empty valid-connections)))))

(api/defendpoint GET "/connections"
  "Get the SFTPGo connections."
  []
  (setting/get :sftpgo-auth-connections))

(api/defendpoint GET "/folders"
  "Get the folder tree from SFTPGo"
                 ;; print all query parameters
  [& query-params]
  (sftpgo/get-folder-tree "." (:connection query-params)))

(api/define-routes)
