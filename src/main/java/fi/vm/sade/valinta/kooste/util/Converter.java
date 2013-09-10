package fi.vm.sade.valinta.kooste.util;

import fi.vm.sade.service.hakemus.schema.AvainArvoTyyppi;
import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.hakemus.schema.HakukohdeTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;

import java.util.HashMap;
import java.util.Map;

/**
 * User: wuoti
 * Date: 9.9.2013
 * Time: 10.08
 */
public class Converter {

    private final static String ETUNIMET = "Etunimet";
    private final static String SUKUNIMI = "Sukunimi";
    private final static String PREFERENCE = "preference";
    private final static String KOULUTUS_ID = "Koulutus-id";
    private final static String DISCRETIONARY = "discretionary";

    private final static String EI_ARVOSANAA = "Ei arvosanaa";

    private static class Hakutoive {
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

    /**
     * Poistaa "Ei arvosanaa" -kentät hakemukselta. Tämän funkkarin voi poistaa kunhan hakemuspalveluun saadaan
     * tehtyä filtteri näille kentille
     *
     * @param arvo
     * @return
     */
    private static String sanitizeArvo(String arvo) {
        if (arvo != null && EI_ARVOSANAA.equals(arvo)) {
            return "";
        }

        return arvo;
    }

    public static HakemusTyyppi hakemusToHakemusTyyppi(Hakemus hakemus) {
        HakemusTyyppi hakemusTyyppi = new HakemusTyyppi();
        hakemusTyyppi.setHakemusOid(hakemus.getOid());
        hakemusTyyppi.setHakijaOid(hakemus.getPersonOid());

        if (hakemus.getAnswers() != null) {

            if (hakemus.getAnswers().getHenkilotiedot() != null) {
                hakemusTyyppi.setHakijanEtunimi(hakemus.getAnswers().getHenkilotiedot().get(ETUNIMET));
                hakemusTyyppi.setHakijanSukunimi(hakemus.getAnswers().getHenkilotiedot().get(SUKUNIMI));

                for (Map.Entry<String, String> e : hakemus.getAnswers().getHenkilotiedot().entrySet()) {
                    AvainArvoTyyppi aa = new AvainArvoTyyppi();
                    aa.setAvain(e.getKey());
                    aa.setArvo(sanitizeArvo(e.getValue()));

                    hakemusTyyppi.getAvainArvo().add(aa);
                }
            }

            Map<Integer, Hakutoive> hakutoiveet = new HashMap<Integer, Hakutoive>();
            if (hakemus.getAnswers().getHakutoiveet() != null) {
                for (Map.Entry<String, String> e : hakemus.getAnswers().getHakutoiveet().entrySet()) {
                    AvainArvoTyyppi aa = new AvainArvoTyyppi();
                    aa.setAvain(e.getKey());
                    aa.setArvo(sanitizeArvo(e.getValue()));

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
            }

            if (hakemus.getAnswers().getKoulutustausta() != null) {
                for (Map.Entry<String, String> e : hakemus.getAnswers().getKoulutustausta().entrySet()) {
                    AvainArvoTyyppi aa = new AvainArvoTyyppi();
                    aa.setAvain(e.getKey());
                    aa.setArvo(sanitizeArvo(e.getValue()));

                    hakemusTyyppi.getAvainArvo().add(aa);
                }
            }

            if (hakemus.getAnswers().getLisatiedot() != null) {
                for (Map.Entry<String, String> e : hakemus.getAnswers().getLisatiedot().entrySet()) {
                    AvainArvoTyyppi aa = new AvainArvoTyyppi();
                    aa.setAvain(e.getKey());
                    aa.setArvo(sanitizeArvo(e.getValue()));

                    hakemusTyyppi.getAvainArvo().add(aa);
                }
            }

            if (hakemus.getAnswers().getOsaaminen() != null) {
                for (Map.Entry<String, String> e : hakemus.getAnswers().getOsaaminen().entrySet()) {
                    AvainArvoTyyppi aa = new AvainArvoTyyppi();
                    aa.setAvain(e.getKey());
                    aa.setArvo(sanitizeArvo(e.getValue()));

                    hakemusTyyppi.getAvainArvo().add(aa);
                }
            }
        }

        if (hakemus.getAdditionalInfo() != null) {
            for (Map.Entry<String, String> e : hakemus.getAdditionalInfo().entrySet()) {
                AvainArvoTyyppi aa = new AvainArvoTyyppi();
                aa.setAvain(e.getKey());
                aa.setArvo(e.getValue());

                hakemusTyyppi.getAvainArvo().add(aa);
            }
        }

        return hakemusTyyppi;
    }
}
