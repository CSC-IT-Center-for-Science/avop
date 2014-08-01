;; Copyright (c) 2013 The Finnish National Board of Education - Opetushallitus
;;
;; This program is free software:  Licensed under the EUPL, Version 1.1 or - as
;; soon as they will be approved by the European Commission - subsequent versions
;; of the EUPL (the "Licence");
;;
;; You may not use this work except in compliance with the Licence.
;; You may obtain a copy of the Licence at: http://www.osor.eu/eupl/
;;
;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; European Union Public Licence for more details.

(ns aipal.asetukset
  (:require
    [schema.core :as s]
    [oph.common.infra.asetukset :refer [lue-asetukset]]))

(def Asetukset
  {:server {:port s/Int
            :base-url s/Str}
   :db {:host s/Str
        :port s/Int
        :name s/Str
        :user s/Str
        :password s/Str
        :maximum-pool-size s/Int
        :minimum-pool-size s/Int}
   :cas-auth-server {:url s/Str
                     :unsafe-https Boolean
                     :enabled Boolean}
   :development-mode Boolean
   :logback {:properties-file s/Str}})
 
(def oletusasetukset
  {:server {:port 8082
            :base-url ""}
   :db {:host "127.0.0.1"
        :port 3456
        :name "aipal"
        :user "aipal_user"
        :password "aipal"
        :maximum-pool-size 15
        :minimum-pool-size 3}
   :cas-auth-server {:url "https://localhost:9443/cas-server-webapp-3.5.2"
                     :unsafe-https false
                     :enabled true}
   :development-mode false ; oletusarvoisesti ei olla kehitysmoodissa. Pitää erikseen kääntää päälle jos tarvitsee kehitysmoodia.
   :logback {:properties-file "resources/logback.xml"}})

(defn hae-asetukset 
  ([asetukset] (lue-asetukset  asetukset Asetukset "aipalvastaus.properties"))
  ([] (hae-asetukset oletusasetukset)))
                 
