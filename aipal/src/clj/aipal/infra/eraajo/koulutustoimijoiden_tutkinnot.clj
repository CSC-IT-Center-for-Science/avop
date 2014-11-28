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

(ns aipal.infra.eraajo.kayttajat
  (:require [clojurewerkz.quartzite.conversion :as qc]
            [clojure.tools.logging :as log]
            [aipal.integraatio.aitu :as aitu]
            [korma.db :as db]
            [aipal.arkisto.koulutustoimija :as koulutustoimija-arkisto]
            [aipal.arkisto.tutkinto :as tutkinto-arkisto]
            [aipal.infra.kayttaja.vaihto :refer [with-kayttaja]]
            [aipal.infra.kayttaja.vakiot :refer [integraatio-uid]]))

(defn paivita-koulutustoimijoiden-tutkinnot! []
  (log/info "Aloitetaan koulutustoimijoiden tutkintojen päivitys Aitusta")
  (db/transaction
    (with-kayttaja integraatio-uid nil
      (let [koulutustoimijoiden-tutkinnot (aitu/hae-koulutustoimijoiden-tutkinnot)]
        (koulutustoimija-arkisto/poista-kaikki-koulutustoimijoiden-tutkinnot!)
        (doseq [[y-tunnus tutkintotunnukset] koulutustoimijoiden-tutkinnot
                :when (koulutustoimija-arkisto/hae y-tunnus)
                tutkintotunnus tutkintotunnukset
                :when (tutkinto-arkisto/hae tutkintotunnus)]
          (koulutustoimija-arkisto/lisaa-koulutustoimijalle-tutkinto! y-tunnus tutkintotunnus)))))
  (log/info "Koulutustoimijoiden tutkintojen päivitys Aitusta valmis"))

;; Cloverage ei tykkää `defrecord`eja generoivista makroista, joten hoidetaan
;; `defjob`:n homma käsin.
(defrecord PaivitaKoulutustoimijoidenTutkinnotJob []
   org.quartz.Job
   (execute [this ctx]
     (paivita-koulutustoimijoiden-tutkinnot!)))
