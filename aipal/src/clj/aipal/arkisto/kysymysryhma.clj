;; Copyright (c) 2014 The Finnish National Board of Education - Opetushallitus
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

(ns aipal.arkisto.kysymysryhma
  (:require [korma.core :as sql]
            [aipal.integraatio.sql.korma :as taulut]))

(defn hae-kysymysryhmat [organisaatio]
  (let [organisaatiosuodatus (fn [query]
                               (-> query
                                 (sql/join :inner :kysymysryhma_organisaatio_view (= :kysymysryhma_organisaatio_view.kysymysryhmaid :kysymysryhmaid))
                                 (sql/where (or {:kysymysryhma_organisaatio_view.koulutustoimija organisaatio}
                                                {:kysymysryhma_organisaatio_view.valtakunnallinen true}))))]
    (sql/select taulut/kysymysryhma
      (organisaatiosuodatus)
      (sql/fields :kysymysryhma.kysymysryhmaid :kysymysryhma.nimi_fi :kysymysryhma.nimi_sv :kysymysryhma.selite_fi :kysymysryhma.selite_sv :kysymysryhma.valtakunnallinen)
      (sql/order :muutettuaika :desc))))

(defn lisaa-kysymysryhma! [k]
  (sql/insert taulut/kysymysryhma
    (sql/values k)))

(defn lisaa-kysymys! [k]
  (sql/insert taulut/kysymys
    (sql/values k)))

(defn lisaa-jatkokysymys! [k]
  (sql/insert :jatkokysymys
    (sql/values k)))

(defn lisaa-monivalintavaihtoehto! [v]
  (sql/insert :monivalintavaihtoehto
    (sql/values v)))

(def kysymysryhma-select
  (->
    (sql/select* taulut/kysymysryhma)
    (sql/fields :kysymysryhmaid :nimi_fi :nimi_sv :taustakysymykset :valtakunnallinen)
    (sql/with taulut/kysymys
      (sql/join :left :jatkokysymys (= :jatkokysymys.jatkokysymysid :kysymys.jatkokysymysid))
      (sql/fields :kysymys.kysymysid :kysymys.kysymys_fi :kysymys.kysymys_sv
                  :kysymys.poistettava :kysymys.pakollinen
                  :jatkokysymys.kylla_teksti_fi :jatkokysymys.kylla_teksti_sv
                  :jatkokysymys.ei_teksti_fi :jatkokysymys.ei_teksti_sv)
      (sql/order :kysymys.jarjestys))))

(defn hae [kysymysryhmaid]
  (first
    (-> kysymysryhma-select
      (sql/where {:kysymysryhmaid kysymysryhmaid})
      sql/exec)))

(defn hae-kyselypohjasta [kyselypohjaid]
  (-> kysymysryhma-select
    (sql/join :inner taulut/kysymysryhma-kyselypohja (= :kysymysryhma_kyselypohja.kysymysryhmaid :kysymysryhma.kysymysryhmaid))
    (sql/fields :kysymysryhma_kyselypohja.kyselypohjaid)
    (sql/where {:kysymysryhma_kyselypohja.kyselypohjaid kyselypohjaid})
    (sql/order :kysymysryhma_kyselypohja.jarjestys)
    sql/exec))
