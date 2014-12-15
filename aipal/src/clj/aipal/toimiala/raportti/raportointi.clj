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

(ns aipal.toimiala.raportti.raportointi
  (:require [korma.core :as sql]))

(defn ^:private hae-monivalintavaihtoehdot [kysymysid]
  (->
    (sql/select* :monivalintavaihtoehto)
    (sql/fields :jarjestys :teksti_fi :teksti_sv)
    (sql/where {:kysymysid kysymysid})
    sql/exec))

(defn aseta-eos [vastaukset kentta]
  (for [vastaus vastaukset]
    (if (:en_osaa_sanoa vastaus)
      (assoc vastaus kentta :eos)
      vastaus)))

(defn ^:private kysymyksen-vastaukset
  [kysymys vastaukset]
  (filter (fn [vastaus] (= (:kysymysid vastaus) (:kysymysid kysymys)))
          vastaukset))

(defn jaottele-asteikko
  [vastaukset]
  (merge {1 0, 2 0, 3 0, 4 0, 5 0, :eos 0}
         (frequencies (map :numerovalinta vastaukset))))

(defn jaottele-jatkokysymys-asteikko
  [vastaukset]
  (merge {1 0, 2 0, 3 0, 4 0, 5 0, :eos 0}
         (frequencies (keep :kylla_asteikko vastaukset))))

(defn jaottele-monivalinta
  [vastaukset]
  (frequencies (map :numerovalinta vastaukset)))

(defn jaottele-vaihtoehdot
  [vastaukset]
  (->> vastaukset
    (map (comp keyword :vaihtoehto))
    frequencies
    (merge {:kylla 0, :ei 0, :eos 0})))

(defn ^:private laske-osuus
  [lukumaara yhteensa]
  (if (> yhteensa 0)
    (/ lukumaara yhteensa)
    0))

(defn prosentteina
  [osuus]
  (Math/round (double (* osuus 100))))

(defn muodosta-asteikko-jakauman-esitys
  [jakauma]
  (let [yhteensa (reduce + (vals jakauma))
        tiedot-vaihtoehdolle (fn [avain lukumaara]
                               {:vaihtoehto-avain avain
                                :lukumaara lukumaara
                                :osuus (prosentteina
                                         (laske-osuus (or lukumaara 0) yhteensa))})]
    [(tiedot-vaihtoehdolle "1" (jakauma 1))
     (tiedot-vaihtoehdolle "2" (jakauma 2))
     (tiedot-vaihtoehdolle "3" (jakauma 3))
     (tiedot-vaihtoehdolle "4" (jakauma 4))
     (tiedot-vaihtoehdolle "5" (jakauma 5))
     (tiedot-vaihtoehdolle "eos" (jakauma :eos))]))

(defn muodosta-kylla-ei-jakauman-esitys
  [jakauma]
  (let [yhteensa (+ (:kylla jakauma) (:ei jakauma) (:eos jakauma))]
    [{:vaihtoehto-avain "kylla"
      :lukumaara (:kylla jakauma)
      :osuus (prosentteina
               (laske-osuus (:kylla jakauma) yhteensa))}
     {:vaihtoehto-avain "ei"
      :lukumaara (:ei jakauma)
      :osuus (prosentteina
               (laske-osuus (:ei jakauma) yhteensa))}
     {:vaihtoehto-avain "eos"
      :lukumaara (:eos jakauma)
      :osuus (prosentteina
               (laske-osuus (:eos jakauma) yhteensa))}]))

(defn ^:private muodosta-monivalintavaihtoehdot
  [kysymys]
  (let [vaihtoehdot (hae-monivalintavaihtoehdot (:kysymysid kysymys))]
    (concat (sort-by :jarjestys vaihtoehdot) [{:jarjestys :eos}])))

(defn muodosta-monivalinta-jakauman-esitys
  [vaihtoehdot jakauma]
  (let [yhteensa (reduce + (vals jakauma))]
    (for [vaihtoehto vaihtoehdot
          :let [lukumaara (or (jakauma (:jarjestys vaihtoehto)) 0)
                osuus (laske-osuus lukumaara yhteensa)]]
      {:vaihtoehto_fi (:teksti_fi vaihtoehto)
       :vaihtoehto_sv (:teksti_sv vaihtoehto)
       :lukumaara lukumaara
       :osuus (prosentteina osuus)
       :jarjestys (:jarjestys vaihtoehto)})))

(defn ^:private lisaa-asteikon-jakauma
  [kysymys vastaukset]
  (let [vastaukset (aseta-eos vastaukset :numerovalinta)]
    (assoc kysymys :jakauma
           (muodosta-asteikko-jakauman-esitys
             (jaottele-asteikko vastaukset)))))

(defn ^:private lisaa-monivalinnan-jakauma
  [kysymys vastaukset]
  (let [vastaukset (aseta-eos vastaukset :numerovalinta)]
    (assoc kysymys :jakauma
           (muodosta-monivalinta-jakauman-esitys
             (muodosta-monivalintavaihtoehdot kysymys)
             (jaottele-monivalinta vastaukset)))))

(defn keraa-kylla-jatkovastaukset
  [kysymys vastaukset]
  (when (:kylla_kysymys kysymys)
    {:kysymys_fi (:kylla_teksti_fi kysymys)
     :kysymys_sv (:kylla_teksti_sv kysymys)
     :jakauma (butlast (muodosta-asteikko-jakauman-esitys (jaottele-jatkokysymys-asteikko vastaukset))) ;; EOS-vastaus on jakauman viimeinen eikä sitä käytetä jatkovastauksissa
     :vastaustyyppi (:kylla_vastaustyyppi kysymys)}))

(defn keraa-ei-jatkovastaukset
  [kysymys vastaukset]
  (when (:ei_kysymys kysymys)
    (let [ei-vastaukset (keep :ei_vastausteksti vastaukset)]
      {:kysymys_fi (:ei_teksti_fi kysymys)
       :kysymys_sv (:ei_teksti_sv kysymys)
       :vastaukset (for [v ei-vastaukset] {:teksti v})
       :vastaustyyppi "vapaateksti"})))

(defn keraa-jatkovastaukset
  [kysymys vastaukset]
  (when (:jatkokysymysid kysymys)
    {:kylla (keraa-kylla-jatkovastaukset kysymys vastaukset)
     :ei (keraa-ei-jatkovastaukset kysymys vastaukset)}))

(defn ^:private lisaa-vaihtoehtojen-jakauma
  [kysymys vastaukset]
  (let [vastaukset (aseta-eos vastaukset :vaihtoehto)]
    (assoc kysymys
           :jakauma
           (muodosta-kylla-ei-jakauman-esitys
             (jaottele-vaihtoehdot vastaukset))
           :jatkovastaukset
           (keraa-jatkovastaukset kysymys vastaukset))))

(defn ^:private lisaa-vastausten-vapaateksti
  [kysymys vastaukset]
  (assoc kysymys :vastaukset
         (for [v vastaukset] {:teksti (:vapaateksti v)})))

(defn kysymyksen-kasittelija
  [kysymys]
  (case (:vastaustyyppi kysymys)
    "arvosana" lisaa-asteikon-jakauma
    "asteikko" lisaa-asteikon-jakauma
    "kylla_ei_valinta" lisaa-vaihtoehtojen-jakauma
    "likert_asteikko" lisaa-asteikon-jakauma
    "monivalinta" lisaa-monivalinnan-jakauma
    "vapaateksti" lisaa-vastausten-vapaateksti))

(defn suodata-eos-vastaukset [kysymys]
  (if (:eos_vastaus_sallittu kysymys)
    kysymys
    (update-in kysymys [:jakauma] butlast))) ;; EOS-vastaus on aina jakauman viimeinen

(defn valitse-kysymyksen-kentat
  [kysymys]
  (select-keys kysymys [:kysymys_fi
                        :kysymys_sv
                        :jakauma
                        :vastaukset
                        :vastaajien_lukumaara
                        :jatkovastaukset
                        :vastaustyyppi
                        :eos_vastaus_sallittu]))

(defn vastaajien-lukumaara [vastaukset]
  (->> vastaukset
    (map :vastaajaid)
    distinct
    count))

(defn kasittele-kysymykset
  [kysymykset vastaukset]
  (for [kysymys kysymykset
        :let [vastaukset-kysymykseen (kysymyksen-vastaukset kysymys vastaukset)
              vastaajia (vastaajien-lukumaara vastaukset-kysymykseen)
              kasitelty-kysymys ((kysymyksen-kasittelija kysymys) kysymys vastaukset-kysymykseen)]]
    (-> kasitelty-kysymys
      (assoc :vastaajien_lukumaara vastaajia)
      suodata-eos-vastaukset
      valitse-kysymyksen-kentat)))

(defn kasittele-kysymysryhmat
  [kysymysryhmat vastaukset]
  (for [kysymysryhma kysymysryhmat]
    (update-in kysymysryhma [:kysymykset] #(kasittele-kysymykset % vastaukset))))

(defn ryhmittele-kysymykset-kysymysryhmittain [kysymykset kysymysryhmat]
  (let [kysymysryhmien-kysymykset (group-by :kysymysryhmaid kysymykset)]
    (for [kysymysryhma kysymysryhmat
          :let [kysymykset (get kysymysryhmien-kysymykset (:kysymysryhmaid kysymysryhma))]]
      (assoc kysymysryhma :kysymykset kysymykset))))

(defn muodosta-raportti-vastauksista
  [kysymysryhmat kysymykset vastaukset]
  (kasittele-kysymysryhmat (ryhmittele-kysymykset-kysymysryhmittain kysymykset kysymysryhmat)
                           vastaukset))
