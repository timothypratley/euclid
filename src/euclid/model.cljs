(ns euclid.model
  (:require [reagent.core :as reagent]))

(def example
  [[:point {:x -10 :y 0}]
   [:point {:x 10 :y 0}]
   [:line {:from 0 :to 1}]
   [:circle {:center 0 :circumpoint 1}]
   [:circle {:center 1 :circumpoint 0}]])

(defn add-point [world [x y]]
  (conj world [:point {:x x :y y}]))

(defn add-line [world a b]
  (conj world [:line {:from a :to b}]))

(defn add-circle [world center circumpoint]
  (conj world [:circle {:center center :circumpoint circumpoint}]))

(defn add-intersection [world a b]
  (conj world [:intersection {:a a :b b}]))

(defmulti intersection
          (fn intersection-dispatch [world [a-type a-properties] [b-type b-properties]]
            [a-type b-type]))

;; https://math.stackexchange.com/questions/256100/how-can-i-find-the-points-at-which-two-circles-intersect
(defmethod intersection [:circle :circle] [world [_ a-properties] [_ b-properties]]
  ;;TODO: fill me in please!!
  )

(defmethod intersection [:circle :line] [world [_ a-properties] [_ b-properties]]
  ;;TODO: fill me in please!!
  )

(defmethod intersection [:line :circle] [world a b]
  (intersection b a))

;; https://en.wikipedia.org/wiki/Line%E2%80%93line_intersection#Given_two_points_on_each_line
(defmethod intersection [:line :line] [world [_ {a-from :from a-to :to}] [_ {b-from :from b-to :to}]]
  (let [[_ {x1 :x y1 :y}] (world a-from)
        [_ {x2 :x y2 :y}] (world a-to)
        [_ {x3 :x y3 :y}] (world b-from)
        [_ {x4 :x y4 :y}] (world b-to)]
    [(/ (- (* (- (* x1 y2)
                 (* y1 x2))
              (- x3 x4))
           (* (- x1 x2)
              (- (* x3 y4) (* y3 x4))))
        (- (* (- x1 x2) (- y3 y4))
           (* (- y1 y2) (- x3 x4))))
     (/ (- (* (- (* x1 y2)
                 (* y1 x2))
              (- y3 y4))
           (* (- y1 y2)
              (- (* x3 y4) (* y3 x4))))
        (- (* (- x1 x2) (- y3 y4))
           (* (- y1 y2) (- x3 x4))))]))

(defn coords [world idx]
  (let [[type properties] (world idx)]
    (case type
      :point (let [{:keys [x y]} properties]
               [x y])
      :intersection (let [{:keys [a b]} properties]
                      (intersection world (world a) (world b))))))

(defn square [x]
  (* x x))

(defn distance [x1 y1 x2 y2]
  (Math/sqrt
    (+ (square (- x2 x1))
       (square (- y2 y1)))))
