(ns metabase.sftpgo.files
  "Convenience functions for sending templated sftpgo files."
  (:require
   [clojure.core.cache :as cache]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [hiccup.core :refer [html]]
   [medley.core :as m]
   [metabase.config :as config]
   [metabase.driver :as driver]
   [metabase.driver.util :as driver.u]
   [metabase.public-settings :as public-settings]
   [metabase.pulse.markdown :as markdown]
   [metabase.pulse.parameters :as params]
   [metabase.pulse.render :as render]
   [metabase.pulse.render.body :as body]
   [metabase.pulse.render.image-bundle :as image-bundle]
   [metabase.pulse.render.js-svg :as js-svg]
   [metabase.pulse.render.style :as style]
   [metabase.query-processor.store :as qp.store]
   [metabase.query-processor.streaming :as qp.streaming]
   [metabase.query-processor.streaming.interface :as qp.si]
   [metabase.query-processor.streaming.xlsx :as qp.xlsx]
   [metabase.util.i18n :as i18n :refer [tru]]
   [metabase.util.urls :as urls]
   [stencil.core :as stencil]
   [stencil.loader :as stencil-loader])
  (:import
   (java.io File IOException OutputStream)))

;; Dev only -- disable template caching
(when config/is-dev?
  (alter-meta! #'stencil.core/render-file assoc :style/indent 1)
  (stencil-loader/set-cache (cache/ttl-cache-factory {} :ttl 0)))

(defn- logo-url []
  (let [url (public-settings/application-logo-url)]
    (cond
      (= url "app/assets/img/logo.svg") "http://static.metabase.com/email_logo.png"

      :else nil)))

(defn- icon-bundle
  [icon-name]
  (let [color     (style/primary-color)
        png-bytes (js-svg/icon icon-name color)]
    (-> (image-bundle/make-image-bundle :attachment png-bytes)
        (image-bundle/image-bundle->attachment))))

(defn- button-style [color]
  (str "display: inline-block; "
       "box-sizing: border-box; "
       "padding: 0.5rem 1.375rem; "
       "font-size: 1.063rem; "
       "font-weight: bold; "
       "text-decoration: none; "
       "cursor: pointer; "
       "color: #fff; "
       "border: 1px solid " color "; "
       "background-color: " color "; "
       "border-radius: 4px;"))

;;; Various Context Helper Fns. Used to build Stencil template context

(defn- common-context
  "Context that is used across multiple templates, and that is the same for all users."
  []
  {:applicationName           (public-settings/application-name)
   :applicationColor          (style/primary-color)
   :applicationLogoUrl        (logo-url)
   :buttonStyle               (button-style (style/primary-color))
   :colorTextLight            style/color-text-light
   :colorTextMedium           style/color-text-medium
   :colorTextDark             style/color-text-dark
   :notificationManagementUrl (urls/notification-management-url)
   :siteUrl                   (public-settings/site-url)})

;;; ### Public Interface

(defn- make-message-attachment [[content-id url]]
  {:type         :inline
   :content-id   content-id
   :content-type "image/png"
   :content      url})

(defn- pulse-link-context
  [{:keys [cards dashboard_id]}]
  (when-let [dashboard-id (or dashboard_id
                              (some :dashboard_id cards))]
    {:pulseLink (urls/dashboard-url dashboard-id)}))

(defn- pulse-context [pulse dashboard]
  (merge (common-context)
         {:emailType                 "pulse"
          :title                     (:name pulse)
          :titleUrl                  (params/dashboard-url (:id dashboard) (params/parameters pulse dashboard))
          :dashboardDescription      (:description dashboard)
          :creator                   (-> pulse :creator :common_name)
          :sectionStyle              (style/style (style/section-style))}
         (pulse-link-context pulse)))

(defn- create-temp-file
  "Separate from `create-temp-file-or-throw` primarily so that we can simulate exceptions in tests"
  [suffix]
  (doto (File/createTempFile "metabase_attachment" suffix)
    .deleteOnExit))

(defn- create-temp-file-or-throw
  "Tries to create a temp file, will give the users a better error message if we are unable to create the temp file"
  [suffix]
  (try
    (create-temp-file suffix)
    (catch IOException e
      (let [ex-msg (tru "Unable to create temp file in `{0}` for attachments "
                        (System/getProperty "java.io.tmpdir"))]
        (throw (IOException. ex-msg e))))))

(defn- create-result-attachment-map [export-type card-name ^File attachment-file]
  (let [{:keys [content-type]} (qp.si/stream-options export-type)]
    {:type         :attachment
     :content-type content-type
     :file-name    (format "%s.%s" card-name (name export-type))
     :content      (-> attachment-file .toURI .toURL)
     :description  (format "More results for '%s'" card-name)}))

(defn- include-csv-attachment?
  "Should this `card` and `results` include a CSV attachment?"
  [{include-csv? :include_csv, include-xls? :include_xls, card-name :name, :as card} {:keys [cols rows], :as result-data}]
  (letfn [(yes [reason & args]
            (log/tracef "Including CSV attachment for Card %s because %s" (pr-str card-name) (apply format reason args))
            true)
          (no [reason & args]
            (log/tracef "NOT including CSV attachment for Card %s because %s" (pr-str card-name) (apply format reason args))
            false)]
    (cond
      include-csv?
      (yes "it has `:include_csv`")

      include-xls?
      (no "it has `:include_xls`")

      (some (complement body/show-in-table?) cols)
      (yes "some columns are not included in rendered results")

      (not= :table (render/detect-pulse-chart-type card nil result-data))
      (no "we've determined it should not be rendered as a table")

      (= (count (take body/rows-limit rows)) body/rows-limit)
      (yes "the results have >= %d rows" body/rows-limit)

      :else
      (no "less than %d rows in results" body/rows-limit))))

(defn- stream-api-results-to-export-format
  "For legacy compatability. Takes QP results in the normal `:api` response format and streams them to a different
  format.

  TODO -- this function is provided mainly because rewriting all of the Pulse/Alert code to stream results directly
  was a lot of work. I intend to rework that code so we can stream directly to the correct export format(s) at some
  point in the future; for now, this function is a stopgap.

  Results are streamed synchronosuly. Caller is responsible for closing `os` when this call is complete."
  [export-format ^OutputStream os {{:keys [rows]} :data, database-id :database_id, :as results}]
  ;; make sure Database/driver info is available for the streaming results writers -- they might need this in order to
  ;; get timezone information when writing results
  (driver/with-driver (driver.u/database->driver database-id)
    (qp.store/with-store
      (qp.store/fetch-and-store-database! database-id)
      (binding [qp.xlsx/*parse-temporal-string-values* true]
        (let [w                           (qp.si/streaming-results-writer export-format os)
              cols                        (-> results :data :cols)
              viz-settings                (-> results :data :viz-settings)
              [ordered-cols output-order] (qp.streaming/order-cols cols viz-settings)
              viz-settings'               (assoc viz-settings :output-order output-order)]
          (qp.si/begin! w
                        (assoc-in results [:data :ordered-cols] ordered-cols)
                        viz-settings')
          (dorun
           (map-indexed
            (fn [i row]
              (qp.si/write-row! w row i ordered-cols viz-settings'))
            rows))
          (qp.si/finish! w results))))))

(defn- result-attachment
  [{{card-name :name, :as card} :card, {{:keys [rows], :as result-data} :data, :as result} :result}]
  (when (seq rows)
    [(when-let [temp-file (and (include-csv-attachment? card result-data)
                               (create-temp-file-or-throw "csv"))]
       (with-open [os (io/output-stream temp-file)]
         (stream-api-results-to-export-format :csv os result))
       (create-result-attachment-map "csv" card-name temp-file))
     (when-let [temp-file (and (:include_xls card)
                               (create-temp-file-or-throw "xlsx"))]
       (with-open [os (io/output-stream temp-file)]
         (stream-api-results-to-export-format :xlsx os result))
       (create-result-attachment-map "xlsx" card-name temp-file))]))

(defn- result-attachments [results]
  (filter some? (mapcat result-attachment results)))

(defn- render-result-card
  [timezone result]
  (if (:card result)
    (render/render-pulse-section timezone result)
    {:content (markdown/process-markdown (:text result) :html)}))

(defn- render-filters
  [notification dashboard]
  (let [filters (params/parameters notification dashboard)
        cells   (map
                 (fn [filter]
                   [:td {:class "filter-cell"
                         :style (style/style {:width "50%"
                                              :padding "0px"
                                              :vertical-align "baseline"})}
                    [:table {:cellpadding "0"
                             :cellspacing "0"
                             :width "100%"
                             :height "100%"}
                     [:tr
                      [:td
                       {:style (style/style {:color style/color-text-medium
                                             :min-width "100px"
                                             :width "50%"
                                             :padding "4px 4px 4px 0"
                                             :vertical-align "baseline"})}
                       (:name filter)]
                      [:td
                       {:style (style/style {:color style/color-text-dark
                                             :min-width "100px"
                                             :width "50%"
                                             :padding "4px 16px 4px 8px"
                                             :vertical-align "baseline"})}
                       (params/value-string filter)]]]])
                 filters)
        rows    (partition 2 2 nil cells)]
    (html
     [:table {:style (style/style {:table-layout :fixed
                                   :border-collapse :collapse
                                   :cellpadding "0"
                                   :cellspacing "0"
                                   :width "100%"
                                   :font-size  "12px"
                                   :font-weight 700
                                   :margin-top "8px"})}
      (for [row rows]
        [:tr {} row])])))

(defn- render-message-body
  [notification message-type message-context timezone dashboard results]
  (let [rendered-cards  (binding [render/*include-title* true]
                          (mapv #(render-result-card timezone %) results))
        icon-name       (case message-type
                          :alert :bell
                          :pulse :dashboard)
        icon-attachment (first (map make-message-attachment (icon-bundle icon-name)))
        filters         (when dashboard
                          (render-filters notification dashboard))
        message-body    (assoc message-context :pulse (html (vec (cons :div (map :content rendered-cards))))
                               :filters filters
                               :iconCid (:content-id icon-attachment))
        attachments     (apply merge (map :attachments rendered-cards))]
    (vec (concat [{:type "text/html; charset=utf-8" :content (stencil/render-file "metabase/sftpgo/pulse" message-body)}]
                 (map make-message-attachment attachments)
                 [icon-attachment]
                 (result-attachments results)))))

(defn- assoc-attachment-booleans [pulse results]
  (for [{{result-card-id :id} :card :as result} results
        :let [pulse-card (m/find-first #(= (:id %) result-card-id) (:cards pulse))]]
    (if result-card-id
      (update result :card merge (select-keys pulse-card [:include_csv :include_xls]))
      result)))

(defn render-pulse-sftpgo
  "Take a pulse object and list of results, returns an array of attachment objects for an sfptgo message."
  [timezone pulse dashboard results]
  (render-message-body pulse 
                       :pulse
                       (pulse-context pulse dashboard)
                       timezone
                       dashboard
                       (assoc-attachment-booleans pulse results)))

