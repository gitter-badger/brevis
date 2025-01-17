(ns brevis.input
  (:import (brevis BrInput)
           (org.lwjgl.input Keyboard))
  (:use [brevis globals display utils osd vector]
        [brevis.physics utils]))

(def mouse-translate-speed 100)

(defn make-input-type
  "Make an input type for input class based upon the input specifications."
  [input-class input-specs]
  (BrInput/makeInputType input-class (java.util.ArrayList. (vals input-specs))))

(defn add-input-handler
  "Add an input handler.
input-class: indicates the class of input. Currently supports (:key-press, :mouse-drag, :mouse-click)"
  [input-class input-specs behavior]
  (let [input-class (if (keyword? input-class) (str (name input-class)) input-class)
        input-type (make-input-type input-class input-specs)
        input-handler (proxy [brevis.BrInput$InputHandler] []
                        (trigger [#^brevis.Engine engine]
                          (behavior)))]
    (.addInputHandler (:input @*gui-state*) input-type input-handler)))

(defn get-mouse-dx
  "Return the current mouse DX."
  []
  (BrInput/getMouseDX))

(defn get-mouse-dy
  "Return the current mouse DY."
  []
  (BrInput/getMouseDY))

#_(def keyspeed 10)
(defn default-input-handlers
  "Define the default input handlers."
  []
  (swap! *gui-state* assoc :keyspeed 1)
  ;(Keyboard/enableRepeatEvents false)
  ;(println "repeat keys?" (Keyboard/areRepeatEventsEnabled))
  (add-input-handler :key-press
                     {:key-id "I"}
                     #(swap! *gui-state* assoc :fullscreen (not (:fullscreen @*gui-state*))))
  (add-input-handler :key-press
                     {:key-id "Q"}
                     #(swap! *gui-state* update-in [:keyspeed] (partial * 1.1)))
  (add-input-handler :key-press
                     {:key-id "E"}
                     #(swap! *gui-state* update-in [:keyspeed] (partial * 0.9)))
  (add-input-handler :key-press
                     {:key-id "X"}
                     (let [timer (atom 0)]
                       (fn []
                         (when (> (- (java.lang.System/nanoTime) @timer) 1000000000) 
                           #_(println "hit pause" (:paused @*gui-state*) @timer (- (java.lang.System/nanoTime) @timer))
                           (reset! timer (java.lang.System/nanoTime))
                           (swap! *gui-state* update-in [:paused] #(not %))))))
  #_(add-input-handler :key-press
                     {:key-id "W"}
                     #(.processKeyboard (:camera @*gui-state*) keyspeed 1 true false false false false false))
  (add-input-handler :key-press
                     {:key-id "D"}
                     #(.moveFromLook (:camera @*gui-state*) (- (:keyspeed @*gui-state*)) 0 0 ))                     
  #_(add-input-handler :key-press
                     {:key-id "A"}
                     #(.processKeyboard (:camera @*gui-state*) keyspeed 1 false false true false false false))
  (add-input-handler :key-press
                     {:key-id "W"}
                     #(.moveFromLook (:camera @*gui-state*) 0 0 (:keyspeed @*gui-state*)))                     
  #_(add-input-handler :key-press
                     {:key-id "S"}
                     #(.processKeyboard (:camera @*gui-state*) keyspeed 1 false true false false false false))
  (add-input-handler :key-press
                     {:key-id "A"}
                     #(.moveFromLook (:camera @*gui-state*) (:keyspeed @*gui-state*) 0 0))                     
  #_(add-input-handler :key-press
                     {:key-id "D"}
                     #(.processKeyboard (:camera @*gui-state*) keyspeed 1 false false false true false false))
  (add-input-handler :key-press
                     {:key-id "S"}
                     #(.moveFromLook (:camera @*gui-state*) 0 0 (- (:keyspeed @*gui-state*))))                     
  #_(add-input-handler :key-press
                     {:key-id "C"}
                     #(.processKeyboard (:camera @*gui-state*) keyspeed 1 false false false false true false))
  (add-input-handler :key-press
                     {:key-id "C"}
                     #(.moveFromLook (:camera @*gui-state*) 0 (- (:keyspeed @*gui-state*)) 0))
  #_(add-input-handler :key-press
                     {:key-id "LSHIFT"}
                     #(.processKeyboard (:camera @*gui-state*) keyspeed 1 false false false false false true))
  (add-input-handler :key-press
                     #_{:key-id "LSHIFT"} ;; too annoying with os x
                     {:key-id "Z"}
                     #_#(.moveFromLook (:camera @*gui-state*) 0 (- keyspeed) 0)
                     #(.moveFromLook (:camera @*gui-state*) 0 (:keyspeed @*gui-state*) 0))
  (add-input-handler :key-press
                     {:key-id "P"}
                     #(swap! *gui-state* assoc :pause (not (:pause @*gui-state*))))
  (add-input-handler :key-press
                     {:key-id "O"}
                     #(screenshot (str "brevis_screenshot_" (System/currentTimeMillis) ".png")))
  (add-input-handler :key-press
                     {:key-id "ESCAPE"}
                     #(swap! *gui-state* assoc :close-requested true))
  (add-input-handler :mouse-click
                     {:mouse-button "LEFT"}
                     #(.rotateFromLook (:camera @*gui-state*) (- (get-mouse-dy)) (get-mouse-dx) 0)))

(defn osd-view-transformation
  "Display the current view transformation as an OSD message."
  []
  (let [cam (:camera @*gui-state*)
        t (get-time)]
    (osd :msg-type :penumbra-rotate 
         :fn (fn [[dt t] state] (str "Rotation: (" 
                                     (.roll cam) "," (.pitch cam) "," (.yaw cam) ")")) 
         :start-t t :stop-t (+ t 1))
    (osd :msg-type :penumbra-translate 
         :fn (fn [[dt t] state] (str "Translation: (" 
                                     (.x cam) "," (.y cam) "," (.z cam) ")"))                                      
         :start-t t :stop-t (+ t 1))))

;; ## Input handling
(defn mouse-drag
  "Rotate the world."
  [[dx dy] _ button state]
  (let [cam (:camera @*gui-state*)
        ;rot-axis (normalize (vec3 (.pitch cam) (.yaw cam) (.roll cam))#_(vec3 (:rot-x @*gui-state*) (:rot-y @*gui-state*) (:rot-z @*gui-state*)))
        ;cam-position (.position cam) #_(vec3 (:shift-x @*gui-state*) (:shift-y @*gui-state*) (:shift-z @*gui-state*))
        ;temp (do (println rot-axis cam-position (vec3 (.pitch cam) (.yaw cam) (.roll cam))))
        ;mouse-x-axis (cross rot-axis (vec3 0 1 0))
        ;mouse-y-axis (cross mouse-x-axis rot-axis)
        
;        rads (/ (Math/PI) 180)
;        thetaY (*(:rot-y @*gui-state*) rads)
;        sY (sin thetaY)
;        cY (cos thetaY)
;        thetaX (* (:rot-x @*gui-state*) rads)
;        sX (sin thetaX)
;        cX (cos thetaX)
        t (get-time)
;        
;        side (* 0.01 dx)
;        fwd (if (= :right button) (* 0.01 dy) 0)
]
    (cond 
      ; Rotate
      (= :left button)       
      (.processMouse cam dx dy (/ (+ (if (pos? dx) (- dx) dx) 
                                     (if (pos? dy) (- dy) dy))
                                  5) 180 -180)      
      ; Translate
      (= :right button)
      (do (cond
            (pos? dx) (.processKeyboard (:camera @*gui-state*) dx mouse-translate-speed false false false true false false)            
            (neg? dx) (.processKeyboard (:camera @*gui-state*) (- dx) mouse-translate-speed false false true false false false))
        (cond 
          (pos? dy) (.processKeyboard (:camera @*gui-state*) dy mouse-translate-speed false false false false true false)
          (neg? dy) (.processKeyboard (:camera @*gui-state*) (- dy) mouse-translate-speed false false false false false true)))      
      ; Zoom
      (= :center button)
      (cond 
          (pos? dy) (.processKeyboard (:camera @*gui-state*) dx mouse-translate-speed true false false false false false)
          (neg? dy) (.processKeyboard (:camera @*gui-state*) (- dx) mouse-translate-speed false true false false false false)))
   (osd-view-transformation)
  state))

(defn mouse-wheel
  "Respond to a mousewheel movement. dw is +/- depending on scroll-up or down."
  [dw state]
  (let [t (get-time)]
    (cond 
      (pos? dw) (.processKeyboard (:camera @*gui-state*) dw mouse-translate-speed true false false false false false)
      (neg? dw) (.processKeyboard (:camera @*gui-state*) (- dw) mouse-translate-speed false true false false false false))
    (osd-view-transformation)
    #_(osd :msg-type :penumbra-rotate 
         :fn (fn [[dt t] state] (str "Rotation: (" 
                                     (:rot-x @*gui-state*) "," (:rot-y @*gui-state*) "," (:rot-z @*gui-state*) ")")) 
         :start-t t :stop-t (+ t 1))
    #_(osd :msg-type :penumbra-translate 
         :fn (fn [[dt t] state] (str "Translation: (" 
                                     (int (:shift-x @*gui-state*)) "," (int (:shift-y @*gui-state*)) "," (int (:shift-z @*gui-state*)) ")")) 
         :start-t t :stop-t (+ t 1))
    state))

(defn mouse-move
  "Respond to a change in x,y position of the mouse."
  [[[dx dy] [x y]] state] 
  (println "mouse-move" x y)
  state)
    
(defn mouse-up
  "Respond to a mouse button being released"
  [[x y] button state] 
  (println "mouse-up" button) 
  state)
    
(defn mouse-click
  "Respond to a click?"
  [[x y] button state]
  (println "mouse-click" button)
  state)

(defn mouse-down
  "Respond to a mouse button being pressed"
  [[x y] button state]
  (println "mouse-down" button)
  state)
