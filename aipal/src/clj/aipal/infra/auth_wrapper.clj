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

(ns aipal.infra.auth-wrapper
  "Authentication middleware. Sitoo käyttäjätunnuksen requestille ennen kuin tietokantayhteys on avattu."
  (:require [ring.util.response :refer [redirect]]
            [compojure.core :as c]
            [oph.korma.korma-auth :as ka]
            [aipal.toimiala.kayttajaoikeudet :as ko]
            [aipal.infra.kayttaja-arkisto :as kayttaja-arkisto]
            [aipal.arkisto.kayttajaoikeus :as kayttajaoikeus-arkisto]
            [clojure.tools.logging :as log]))

(defn get-userid-from-request
  "cas filter laittaa :username nimiseen propertyyn käyttäjätunnuksen"
  [request]
  {:post [(not (nil? %))]}
  (:username request))

(defn wrap-sessionuser [ring-handler]
  (fn [request]
    (let [userid (get-userid-from-request request)
          impersonoitu-oid (get-in request [:session :impersonoitu-oid])
          _ (log/debug "userid set to " userid ", impersonated oid " impersonoitu-oid)]
      (binding [ka/*current-user-uid* userid
                ka/*current-user-oid* (promise)
                ka/*impersonoitu-oid* impersonoitu-oid]
        (let [kayttaja (kayttaja-arkisto/hae-uid userid)
              oikeudet (kayttajaoikeus-arkisto/hae-oikeudet (:oid kayttaja))
              kayttajatiedot {:kayttajan_nimi (str (:etunimi kayttaja) " " (:sukunimi kayttaja))}
              auth-map (assoc kayttajatiedot :roolit (:roolit oikeudet))]
          (log/info "käyttäjä autentikoitu " auth-map (when ka/*impersonoitu-oid* (str ": impersonoija=" ka/*current-user-uid*)))
          (binding [ko/*current-user-authmap* auth-map]
            (ring-handler request)))))))
