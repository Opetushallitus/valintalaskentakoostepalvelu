package fi.vm.sade.valinta.kooste.valintakokeet.komponentti;

import fi.vm.sade.service.hakemus.schema.AvainArvoTyyppi;
import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.hakemus.schema.HakukohdeTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.valinta.kooste.paasykokeet.komponentti.proxy.HakukohteenValintaperusteetProxy;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
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

    @Autowired
    private ApplicationResource applicationResource;

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

    public void laske(@Simple("${property.hakemusOid}") String hakemusOid) {
        LOG.info("Lasketaan valintakoeosallistumiset hakemukselle " + hakemusOid);

        Hakemus hakemus = applicationResource.getApplicationByOid(hakemusOid);

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
