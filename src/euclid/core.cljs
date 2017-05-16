(enable-console-print!)

(ns euclid.core
  (:require
    [euclid.model :as model]
    [euclid.view :as view]
    [reagent.core :as reagent]))


(defonce app-state (reagent/atom []))

(reagent/render-component
  [view/main app-state]
  (js/document.getElementById "app"))

(defn on-js-reload [])
