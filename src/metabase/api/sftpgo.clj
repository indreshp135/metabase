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
  [:as {{:keys [sftpgo-auth-url
                sftpgo-auth-enabled
                sftpgo-auth-username
                sftpgo-auth-password]} :body}]
  {sftpgo-auth-url                         (s/maybe s/Str)
   sftpgo-auth-enabled                     (s/maybe s/Bool)
   sftpgo-auth-username                    (s/maybe s/Str)
   sftpgo-auth-password                    (s/maybe s/Str)}
  (db/transaction
   (setting/set-many! {:sftpgo-auth-url                         sftpgo-auth-url
                       :sftpgo-auth-username                    sftpgo-auth-username
                       :sftpgo-auth-password                    sftpgo-auth-password})
   (sftpgo/sftpgo-auth-enabled! sftpgo-auth-enabled)))


#_{:clj-kondo/ignore [:deprecated-var]}
(api/defendpoint GET "/get-folder-tree"
  "Get the folder tree from SFTPGo"
  []
  (sftpgo/get-folder-tree "."))

(api/define-routes)
