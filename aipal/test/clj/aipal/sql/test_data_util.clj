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

(ns aipal.sql.test-data-util
  (:require [aipal.arkisto.vastaajatunnus]
    [aipal.arkisto.kysely]
    [aipal.arkisto.kyselykerta]
    [aipal.arkisto.koulutustoimija]
    [aipal.arkisto.vastaajatunnus]
    [clj-time.core :as time]
    [clj-time.core :as ctime]
    [korma.core :as sql]
    [oph.korma.korma :refer [joda-datetime->sql-timestamp]]))

(def default-koulutusala
  {:koulutusalatunnus "1"
   :nimi_fi "Koulutusala"})

(def default-opintoala
  {:opintoalatunnus "123"
   :koulutusala "1"
   :nimi_fi "Opintoala"})

(def  default-tutkinto
  {:tutkintotunnus "123456"
   :nimi_fi "Autoalan perustutkinto"
   :opintoala "123"})

(def default-koulutustoimija
  {:ytunnus "1234567-8"
   :nimi_fi "Pörsänmäen opistokeskittymä"})

(defn lisaa-koulutusala!
  ([koulutusala]
    (let [k (merge default-koulutusala koulutusala)]
      (sql/insert :koulutusala
        (sql/values k))))
  ([]
    (lisaa-koulutusala! default-koulutusala)))

(defn lisaa-opintoala!
  ([opintoala]
    (let [o (merge default-opintoala opintoala)]
      (sql/insert :opintoala
        (sql/values o))))
  ([]
    (lisaa-koulutusala!)
    (lisaa-opintoala! default-opintoala)))

(defn lisaa-tutkinto!
  ([tutkinto]
    (let [t (merge default-tutkinto tutkinto)]
      (sql/insert :tutkinto
        (sql/values t))))
  ([]
    (lisaa-opintoala!)
    (lisaa-tutkinto! default-tutkinto)))

(defn lisaa-koulutustoimija!
  ([koulutustoimija]
    (let [t (merge default-koulutustoimija koulutustoimija)]
      (sql/insert :koulutustoimija
        (sql/values t))))
  ([]
    (lisaa-koulutustoimija! default-koulutustoimija)))

(defn anna-koulutustoimija!
  "Palauttaa koulutustoimijan kannasta tai lisää uuden"
  []
  (let [k (aipal.arkisto.koulutustoimija/hae-kaikki)]
    (or (first k)
      (aipal.arkisto.koulutustoimija/lisaa! default-koulutustoimija))))

(defn lisaa-kysely!
  []
  (let [koulutustoimija (anna-koulutustoimija!)]
    (aipal.arkisto.kysely/lisaa! {:nimi_fi "oletuskysely, testi"
                                  :koulutustoimija (:ytunnus koulutustoimija)
                                  })))

(defn lisaa-kyselykerta!
  []
  (let [kysely (lisaa-kysely!)]
    (aipal.arkisto.kyselykerta/lisaa! (:kyselyid kysely) {:kyselyid (:kyselyid kysely)
                                                          :nimi_fi "oletuskyselykerta, testi"
                                                          :voimassa_alkupvm (joda-datetime->sql-timestamp (ctime/now))
                                                          :voimassa_loppupvm (joda-datetime->sql-timestamp (ctime/now))
                                                          })))

(defn lisaa-vastaajatunnus!
  []
  (let [kyselykerta (lisaa-kyselykerta!)]
    (aipal.arkisto.vastaajatunnus/lisaa! (:kyselykertaid kyselykerta) nil nil nil nil)))
