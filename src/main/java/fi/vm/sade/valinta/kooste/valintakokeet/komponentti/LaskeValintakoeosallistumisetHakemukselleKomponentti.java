package fi.vm.sade.valinta.kooste.valintakokeet.komponentti;

import com.google.gson.Gson;
import fi.vm.sade.service.hakemus.schema.AvainArvoTyyppi;
import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.hakemus.schema.HakukohdeTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.valinta.kooste.paasykokeet.komponentti.proxy.HakukohteenValintaperusteetProxy;
import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * User: wuoti
 * Date: 29.8.2013
 * Time: 15.33
 */
@Component("laskeValintakoeosallistumisetHakemukselleKomponentti")
public class LaskeValintakoeosallistumisetHakemukselleKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(LaskeValintakoeosallistumisetHakemukselleKomponentti.class);

    @Autowired
    private HakukohteenValintaperusteetProxy proxy;

    @Autowired
    private ValintalaskentaService valintalaskentaService;

    private class Answers {
        private Map<String, String> henkilotiedot = new HashMap<String, String>();
        private Map<String, String> lisatiedot = new HashMap<String, String>();
        private Map<String, String> hakutoiveet = new HashMap<String, String>();
        private Map<String, String> koulutustausta = new HashMap<String, String>();
        private Map<String, String> osaaminen = new HashMap<String, String>();

        private Map<String, String> getHenkilotiedot() {
            return henkilotiedot;
        }

        private void setHenkilotiedot(Map<String, String> henkilotiedot) {
            this.henkilotiedot = henkilotiedot;
        }

        private Map<String, String> getLisatiedot() {
            return lisatiedot;
        }

        private void setLisatiedot(Map<String, String> lisatiedot) {
            this.lisatiedot = lisatiedot;
        }

        private Map<String, String> getHakutoiveet() {
            return hakutoiveet;
        }

        private void setHakutoiveet(Map<String, String> hakutoiveet) {
            this.hakutoiveet = hakutoiveet;
        }

        private Map<String, String> getKoulutustausta() {
            return koulutustausta;
        }

        private void setKoulutustausta(Map<String, String> koulutustausta) {
            this.koulutustausta = koulutustausta;
        }

        private Map<String, String> getOsaaminen() {
            return osaaminen;
        }

        private void setOsaaminen(Map<String, String> osaaminen) {
            this.osaaminen = osaaminen;
        }
    }

    private class Hakemus {
        private String type;
        private String applicationSystemId;
        private Answers answers;

        private String oid;
        private String state;
        private String personOid;

        private String getType() {
            return type;
        }

        private void setType(String type) {
            this.type = type;
        }

        private String getApplicationSystemId() {
            return applicationSystemId;
        }

        private void setApplicationSystemId(String applicationSystemId) {
            this.applicationSystemId = applicationSystemId;
        }

        private Answers getAnswers() {
            return answers;
        }

        private void setAnswers(Answers answers) {
            this.answers = answers;
        }

        private String getOid() {
            return oid;
        }

        private void setOid(String oid) {
            this.oid = oid;
        }

        private String getState() {
            return state;
        }

        private void setState(String state) {
            this.state = state;
        }

        private String getPersonOid() {
            return personOid;
        }

        private void setPersonOid(String personOid) {
            this.personOid = personOid;
        }
    }

    private final static String ETUNIMET = "Etunimet";
    private final static String SUKUNIMI = "Sukunimi";
    private final static String PREFERENCE = "preference";
    private final static String KOULUTUS_ID = "Koulutus-id";
    private final static String DISCRETIONARY = "discretionary";

    private class Hakutoive {
        private Boolean harkinnanvaraisuus;
        private String hakukohdeOid;

        private Boolean getHarkinnanvaraisuus() {
            return harkinnanvaraisuus;
        }

        private void setHarkinnanvaraisuus(Boolean harkinnanvaraisuus) {
            this.harkinnanvaraisuus = harkinnanvaraisuus;
        }

        private String getHakukohdeOid() {
            return hakukohdeOid;
        }

        private void setHakukohdeOid(String hakukohdeOid) {
            this.hakukohdeOid = hakukohdeOid;
        }
    }

    public void laske(@Simple("${property.hakemusJson}") String hakemusJson) {
        Hakemus hakemus = new Gson().fromJson(hakemusJson, Hakemus.class);

        HakemusTyyppi hakemusTyyppi = new HakemusTyyppi();
        hakemusTyyppi.setHakemusOid(hakemus.getOid());
        hakemusTyyppi.setHakijanEtunimi(hakemus.getAnswers().getHenkilotiedot().get(ETUNIMET));
        hakemusTyyppi.setHakijanSukunimi(hakemus.getAnswers().getHenkilotiedot().get(SUKUNIMI));
        hakemusTyyppi.setHakijaOid(hakemus.getPersonOid());

        Map<Integer, Hakutoive> hakutoiveet = new HashMap<Integer, Hakutoive>();
        for (Map.Entry<String, String> e : hakemus.getAnswers().getHakutoiveet().entrySet()) {
            AvainArvoTyyppi aa = new AvainArvoTyyppi();
            aa.setAvain(e.getKey());
            aa.setArvo(e.getValue());

            hakemusTyyppi.getAvainArvo().add(aa);

            if (e.getKey().startsWith(PREFERENCE)) {
                Integer prioriteetti = Integer.valueOf(e.getKey().replaceAll("\\D+", ""));

                Hakutoive hakutoive = null;
                if (!hakutoiveet.containsKey(prioriteetti)) {
                    hakutoive = new Hakutoive();
                    hakutoiveet.put(prioriteetti, hakutoive);
                } else {
                    hakutoive = hakutoiveet.get(prioriteetti);
                }

                if (e.getKey().endsWith(KOULUTUS_ID)) {
                    hakutoive.setHakukohdeOid(e.getValue());
                } else if (e.getKey().endsWith(DISCRETIONARY)) {
                    Boolean discretionary = Boolean.valueOf(e.getValue());
                    discretionary = discretionary == null ? false : discretionary;

                    hakutoive.setHarkinnanvaraisuus(discretionary);
                }
            }
        }

        for (Map.Entry<Integer, Hakutoive> e : hakutoiveet.entrySet()) {
            Hakutoive hakutoive = e.getValue();
            if (hakutoive.getHakukohdeOid() != null && !hakutoive.getHakukohdeOid().trim().isEmpty()) {
                HakukohdeTyyppi hk = new HakukohdeTyyppi();
                hk.setHakukohdeOid(e.getValue().getHakukohdeOid());
                hk.setHarkinnanvaraisuus(e.getValue().getHarkinnanvaraisuus());
                hk.setPrioriteetti(e.getKey());
                hakemusTyyppi.getHakutoive().add(hk);
            }
        }

        for (Map.Entry<String, String> e : hakemus.getAnswers().getHenkilotiedot().entrySet()) {
            AvainArvoTyyppi aa = new AvainArvoTyyppi();
            aa.setAvain(e.getKey());
            aa.setArvo(e.getValue());

            hakemusTyyppi.getAvainArvo().add(aa);
        }

        for (Map.Entry<String, String> e : hakemus.getAnswers().getKoulutustausta().entrySet()) {
            AvainArvoTyyppi aa = new AvainArvoTyyppi();
            aa.setAvain(e.getKey());
            aa.setArvo(e.getValue());

            hakemusTyyppi.getAvainArvo().add(aa);
        }

        for (Map.Entry<String, String> e : hakemus.getAnswers().getLisatiedot().entrySet()) {
            AvainArvoTyyppi aa = new AvainArvoTyyppi();
            aa.setAvain(e.getKey());
            aa.setArvo(e.getValue());

            hakemusTyyppi.getAvainArvo().add(aa);
        }

        for (Map.Entry<String, String> e : hakemus.getAnswers().getOsaaminen().entrySet()) {
            AvainArvoTyyppi aa = new AvainArvoTyyppi();
            aa.setAvain(e.getKey());
            aa.setArvo(e.getValue());

            hakemusTyyppi.getAvainArvo().add(aa);
        }

        Set<String> hakutoiveOids = new HashSet<String>();
        for (HakukohdeTyyppi ht : hakemusTyyppi.getHakutoive()) {
            hakutoiveOids.add(ht.getHakukohdeOid());
        }

        List<ValintaperusteetTyyppi> valintaperusteet = proxy.haeValintaperusteet(hakutoiveOids);
        valintalaskentaService.valintakokeet(hakemusTyyppi, valintaperusteet);
    }
}
