(ns comportexviz.demos.letters
  (:require [org.nfrac.comportex.demos.letters :as demo]
            [org.nfrac.comportex.core :as core]
            [org.nfrac.comportex.util :as util]
            [comportexviz.main :as main]
            [comportexviz.helpers :as helpers]
            [comportexviz.bridge.browser :as server]
            [comportexviz.server.data :as data]
            [comportexviz.util :as utilv]
            [reagent.core :as reagent :refer [atom]]
            [reagent-forms.core :refer [bind-fields]]
            [goog.dom :as dom]
            [goog.dom.forms :as forms]
            [cljs.core.async :as async :refer [put! <!]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [comportexviz.macros :refer [with-ui-loading-message]]))

(def config
  (atom {:n-regions 1
         :encoder :block
         :world-buffer-count 0}))

(def world-buffer (async/buffer 5000))
(def world-c
  (async/chan world-buffer
              (comp (map (util/keep-history-middleware 300 :value :history))
                    (map #(assoc % :label (:value %))))))

(def model (atom nil))

(def into-sim (async/chan))

(add-watch model ::count-world-buffer
           (fn [_ _ _ _]
             (swap! config assoc :world-buffer-count (count world-buffer))))

(def text-to-send
  (atom
"Jane has eyes.
Jane has a head.
Jane has a mouth.
Jane has a brain.
Jane has a book.
Jane has no friend.

Chifung has eyes.
Chifung has a head.
Chifung has a mouth.
Chifung has a brain.
Chifung has no book.
Chifung has a friend."))

(def max-shown 300)
(def scroll-every 150)

(defn world-pane
  []
  (let [show-predictions (atom false)
        selected-htm (atom nil)]
    (add-watch main/selection ::fetch-selected-htm
               (fn [_ _ _ [sel]]
                 (when-let [model-id (:model-id sel)]
                   (let [out-c (async/chan)]
                     (put! main/into-journal [:get-model model-id out-c])
                     (go
                       (reset! selected-htm (<! out-c)))))))
    (fn []
      (when-let [step (main/selected-step)]
        (when-let [htm @selected-htm]
          (let [inval (:input-value step)]
            [:div
             [:p.muted [:small "Input on selected timestep."]]
             [:div {:style {:min-height "40vh"}}
              (helpers/text-world-input-component inval htm max-shown scroll-every "")]
             [:div.checkbox
              [:label [:input {:type :checkbox
                               :checked (when @show-predictions true)
                               :on-change (fn [e]
                                            (swap! show-predictions not)
                                            (.preventDefault e))}]
               "Compute predictions"]]
             (when @show-predictions
               (helpers/text-world-predictions-component htm 8))]))))))

(defn set-model!
  []
  (with-ui-loading-message
    (let [n-regions (:n-regions @config)
          sensor (case (:encoder @config)
                   :block demo/block-sensor
                   :random demo/random-sensor)
          init? (nil? @model)]
      (reset! model (demo/n-region-model n-regions demo/spec sensor))
      (if init?
        (server/init model world-c main/into-journal into-sim)
        (reset! main/step-template (data/step-template-data @model))))))

(defn immediate-key-down!
  [e]
  (when-let [[x] (->> (or (.-keyCode e) (.-charCode e))
                      (.fromCharCode js/String)
                      (demo/clean-text)
                      (seq))]
    (async/put! world-c {:value (str x)}))
  (swap! config assoc :world-buffer-count (count world-buffer)))

(defn send-text!
  []
  (when-let [xs (seq (demo/clean-text @text-to-send))]
    (async/onto-chan world-c (for [x xs] {:value (str x)})
                     false)
    (swap! config assoc :world-buffer-count (count world-buffer))))

(def config-template
  [:div.form-horizontal
   [:div.form-group
    [:label.col-sm-5 "Number of regions:"]
    [:div.col-sm-7
     [:input.form-control {:field :numeric
                           :id :n-regions}]]]
   [:div.form-group
    [:label.col-sm-5 "Letter encoder:"]
    [:div.col-sm-7
     [:select.form-control {:field :list
                            :id :encoder}
      [:option {:key :block} "block"]
      [:option {:key :random} "random"]]]]
   [:div.form-group
    [:div.col-sm-offset-5.col-sm-7
     [:button.btn.btn-default
      {:on-click (fn [e]
                   (set-model!)
                   (.preventDefault e))}
      "Restart with new model"]
     [:p.text-danger "This resets all parameters."]]]
   ])

(defn model-tab
  []
  [:div
   [:p "In this example, text input is presented as a sequence of letters.
        Allowed characters are letters, numbers, space, period and question
        mark."]

   [:h3 "Input " [:small "Letter sequences"]]
   [:p.text-info
    (str (:world-buffer-count @config) " queued input values.")]
   [:div.well
    "Immediate input as you type: "
    [:input {:size 2 :maxLength 1
             :on-key-press (fn [e]
                             (immediate-key-down! e)
                             (.preventDefault e))}]]
   [:div.well
    [:textarea {:style {:width "90%" :height "10em"}
                :value @text-to-send
                :on-change (fn [e]
                             (reset! text-to-send
                                     (-> e .-target forms/getValue))
                             (.preventDefault e))}]
    [:button.btn.btn-primary {:on-click (fn [e]
                                          (send-text!)
                                          (.preventDefault e))}
     "Send text block input"]]

   [:h3 "HTM model"]
   [bind-fields config-template config]
   ]
  )

(defn ^:export init
  []
  (reagent/render [main/comportexviz-app model-tab world-pane (atom into-sim)]
                  (dom/getElement "comportexviz-app"))
  (put! into-sim [:run])
  (set-model!))
