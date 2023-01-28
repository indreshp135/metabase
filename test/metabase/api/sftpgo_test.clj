(ns metabase.api.sftpgo-test
  (:require
   [clojure.test :refer :all]
   [metabase.integrations.sftpgo :as sftpgo]
   [metabase.test :as mt]))

(deftest get-sftpgo-connections-test
  (testing "GET /connections and PUT /settings"
    (testing "Getting SFTPGo connections with valid data"
      (let [valid-connections [{:url "http://example.com"
                                :username "user"
                                :password "password"
                                :name "connection1"}
                               {:url "http://example2.com"
                                :username "user2"
                                :password "password2"
                                :name "connection2"}]]
        (is (= true (mt/user-http-request :crowberto :put 200 "sftpgo/settings" {:sftpgo-auth-connections valid-connections})))
        (is (= valid-connections (mt/user-http-request :crowberto :get 200 "sftpgo/connections")))
        (is (= "true" (sftpgo/sftpgo-auth-enabled! valid-connections)))))))
