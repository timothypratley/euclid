(ns euclid.model)

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

(defn square [x]
  (* x x))

(defn distance [x1 y1 x2 y2]
  (Math/sqrt
    (+ (square (- x2 x1))
       (square (- y2 y1)))))

(def epsilon
  0.0000001)

(defmulti intersection
          (fn intersection-dispatch [world [a-type a-properties] [b-type b-properties]]
            [a-type b-type]))

;; https://math.stackexchange.com/questions/256100/how-can-i-find-the-points-at-which-two-circles-intersect
;; http://paulbourke.net/geometry/circlesphere/
(defmethod intersection [:circle :circle]
  [world
   [_ {center-a :center circumpoint-a :circumpoint}]
   [_ {center-b :center circumpoint-b :circumpoint}]]
 (let [[_ {x1 :x y1 :y}] (world center-a)
       [_ {x11 :x y11 :y}] (world circumpoint-a)
       r1 (distance x1 y1 x11 y11)
       [_ {x2 :x y2 :y}] (world center-b)
       [_ {x22 :x y22 :y}] (world circumpoint-b)
       r2 (distance x2 y2 x22 y22)
       dx (- x2 x1)
       dy (- y2 y1)
       d (Math/sqrt (+ (square dx) (square dy)))
       l (/ (+ (square r1) (- (square r2)) (square d))
            (* 2 d))
       h (Math/sqrt (- (square r1) (square l)))
       a1 (* (/ l d) dx)
       a2 (* (/ h d) dy)
       b1 (* (/ l d) dy)
       b2 (* (/ h d) dx)]
   (cond
     (> d (+ r1 r2)) []                    ; do not intersect, too far apart
     (< d (Math/abs (- r1 r2))) []         ; one circle is inside the other
     (zero? d) []                          ; coincident (infinite intersections if r0=r1, otherwise none)

     (or (= d (+ r1 r2))
         (= d (Math/abs (- r1 r2))))
     ;; 1 intersection
     [(+ a1 x1)
      (+ b1 y1)]

     :else
     ;; 2 intersections
     [(+ a1 (- a2) x1)
      (+ b1 b2 y1)
      (+ a1 a2 x1)
      (+ b1 (- b2) y1)])))

;; http://paulbourke.net/geometry/circlesphere/
(defmethod intersection [:circle :line]
  [world
   [_ {:keys [center circumpoint]}]
   [_ {:keys [from to]}]]
  (let [[_ {cx :x cy :y}] (world center)
        [_ {rx :x ry :y}] (world circumpoint)
        r (distance cx cy rx ry)
        [_ {x1 :x y1 :y}] (world from)
        [_ {x2 :x y2 :y}] (world to)
        dx (- x2 x1)
        dy (- y2 y1)
        a (+ (square dx) (square dy))
        b (* 2 (+ (* dx (- x1 cx))
                  (* dy (- y1 cy))))
        c (+ (+ (square cx) (square cy))
             (+ (square x1) (square y1))
             (- (* 2 (+ (* cx x1)
                        (* cy y1))))
             (- (square r)))
        bb4ac (- (square b)
                 (* 4 a c))]
    (cond
      (or (< (Math/abs a) epsilon) (< bb4ac 0))
      []

      (zero? bb4ac)
      ;; 1 intersection
      (let [mu (/ (- b) (* 2 a))]
        [(+ x1 (* mu dx))
         (+ y1 (* mu dy))])

      :else
      ;; 2 intersections
      (let [mu1 (/ (+ (- b) (Math/sqrt bb4ac))
                   (* 2 a))
            mu2 (/ (- (- b) (Math/sqrt bb4ac))
                   (* 2 a))]
        [(+ x1 (* mu1 dx))
         (+ y1 (* mu1 dy))
         (+ x1 (* mu2 dx))
         (+ y1 (* mu2 dy))]))))

(defmethod intersection [:line :circle] [world a b]
  (intersection world b a))

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
