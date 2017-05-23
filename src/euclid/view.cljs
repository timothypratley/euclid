(ns euclid.view
  (:require
    [euclid.model :as model]
    [devcards.core :as dc :refer-macros [defcard-rg]]
    [clojure.string :as string]
    [reagent.core :as reagent]))

;; These define the logical coordinates available (-100,-100 to +100,+100)
(def view-box-left -100)
(def view-box-top -100)
(def view-box-width 200)
(def view-box-height 200)

(defn xy [e]
  (let [rect (.getBoundingClientRect (.-target e))]
    [(-> (- (.-clientX e) (.-left rect))
         (/ (.-width rect))
         (* view-box-width)
         (+ view-box-left))
     (-> (- (.-clientY e) (.-top rect))
         (/ (.-height rect))
         (* view-box-height)
         (+ view-box-top))]))

(defmulti draw-element (fn dispatch [world view-state idx [type properties]] type))

(defmethod draw-element :point [world view-state idx [_ {:keys [x y]}]]
  [:circle
   {:r 1
    :cx x
    :cy y
    :stroke "red"
    :fill "red"
    :on-click
    (fn [e]
      (.stopPropagation e)
      (case (:action @view-state)
        "point" nil
        "line" (if-let [from (:from @view-state)]
                 (do (swap! world model/add-line from idx)
                     (swap! view-state dissoc :from))
                 (swap! view-state assoc :from idx))
        "circle" (if-let [center (:center @view-state)]
                   (do (swap! world model/add-circle center idx)
                       (swap! view-state dissoc :center))
                   (swap! view-state assoc :center idx))))}])

(defmethod draw-element :intersection [world view-state idx [_ {:keys [a b]}]]
  (let [coords (model/intersection @world (@world a) (@world b))]
    (into
      [:g]
      (for [[x y] (partition 2 coords)]
        [:circle
         {:r 1
          :cx x
          :cy y
          :stroke "green"
          :fill "green"
          :on-click
          (fn [e]
            (.stopPropagation e)
            (case (:action @view-state)
              "point" nil
              "line" (if-let [from (:from @view-state)]
                       (do (swap! world model/add-line from idx)
                           (swap! view-state dissoc :from))
                       (swap! view-state assoc :from idx))
              "circle" (if-let [center (:center @view-state)]
                         (do (swap! world model/add-circle center idx)
                             (swap! view-state dissoc :center))
                         (swap! view-state assoc :center idx))))}]))))


(defmethod draw-element :line [world view-state idx [_ {:keys [from to]}]]
  (let [[_ {from-x :x from-y :y}] (@world from)
        [_ {to-x :x to-y :y}] (@world to)]
    [:path
     {:d (str "M " from-x "," from-y " L " to-x "," to-y)
      :on-click
      (fn [e]
        (.stopPropagation e)
        (if-let [intersects (:intersects @view-state)]
          (do (swap! world model/add-intersection intersects idx)
              (swap! view-state dissoc :intersects))
          (swap! view-state assoc :intersects idx)))}]))

(defmethod draw-element :circle [world view-state idx [_ {:keys [center circumpoint]}]]
  (let [[cx cy] (model/coords @world center)
        [x2 y2] (model/coords @world circumpoint)]
    [:circle
     {:cx cx
      :cy cy
      :r (model/distance cx cy x2 y2)
      :on-click
      (fn [e]
        (.stopPropagation e)
        (if-let [intersects (:intersects @view-state)]
          (do (swap! world model/add-intersection intersects idx)
              (swap! view-state dissoc :intersects))
          (swap! view-state assoc :intersects idx)))}]))

(defn order-by-z-index [world elements]
  (map
    second
    (sort-by
      (fn [[[type properties] _]]
        ({:point 4
          :intersection 3
          :line 2
          :circle 1}
          type))
      (map vector world elements))))

(defn interactive-svg [world view-state]
  [:svg
   {:width 600
    :height 600
    :view-box (string/join " " [view-box-left view-box-top view-box-width view-box-height])
    :style {:border "1px solid"
            :cursor "crosshair"}
    :on-click
    (fn [e]
      (case (:action @view-state)
        "point" (swap! world model/add-point (xy e))
        "line" nil
        "circle" nil))}
   (into
     [:g
      {:stroke "black"
       :fill "none"}]
     (order-by-z-index
       @world
       (map-indexed #(draw-element world view-state %1 %2) @world)))])

(defcard-rg svg-example
  [interactive-svg
   (reagent/atom model/example)
   (reagent/atom {:action "point"})])

(defn actions-toolbar [view-state]
  [:form
   [:input
    {:type "radio"
     :name "action"
     :value "point"
     :default-checked "true"
     :on-click (fn [e]
                 (reset! view-state {:action "point"}))}]
   "Add point"
   [:br]
   [:input
    {:type "radio"
     :name "action"
     :value "line"
     :on-click (fn [e]
                 (reset! view-state {:action "line"}))}]
   "Add line"
   (when-let [from (:from @view-state)]
     (str " from " from))
   [:br]
   [:input
    {:type "radio"
     :name "action"
     :value "circle"
     :on-click (fn [e]
                 (reset! view-state {:action "circle"}))}]
   "Add circle"
   (when-let [center (:center @view-state)]
     (str " center " center))
   [:br]
   [:div "To add an intersection, click one line then the other"]])

(defmulti list-element (fn list-element-dispatch [[type properties]]
                         type))

;; TODO: humanize
(defmethod list-element :default [element]
  (pr-str element))

(defn list-view [world]
  [:div
   {:style {:float "right"}}
   (into
     [:ul
      {:style {:list-style "none"}}]
     (for [element @world]
       [:li
        [:code (list-element element)]]))])

(defn main [world]
  (let [view-state (reagent/atom {:action "point"})]
    [:div
     [:h1 "Euclid"]
     [actions-toolbar view-state]
     [interactive-svg world view-state]
     [list-view world]]))