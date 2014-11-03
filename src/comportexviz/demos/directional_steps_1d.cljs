(ns comportexviz.demos.directional-steps-1d
  (:require [org.nfrac.comportex.demos.directional-steps-1d :as demo]
            [comportexviz.main :as main])
  (:require-macros [comportexviz.macros :refer [with-ui-loading-message]]))

(defn ^:export set-n-region-model
  [n]
  (with-ui-loading-message
    (main/set-model
     (demo/n-region-model n))))
