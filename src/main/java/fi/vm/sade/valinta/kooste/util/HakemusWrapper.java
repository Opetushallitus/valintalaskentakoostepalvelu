package fi.vm.sade.valinta.kooste.util;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.hakemus.dto.Yhteystiedot;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * @author Jussi Jartamo
 *         <p/>
 *         Hakemustietojen luku hakemustietueesta vikasietoisesti
 */
public class HakemusWrapper {

    private final Hakemus hakemus;
    private Map<String, String> henkilotiedot = null;
    private Map<String, String> lisatiedot = null;
    private Map<String, String> hakutoiveet = null;
    private final static String ETUNIMET = "Etunimet";
    private final static String KUTSUMANIMI = "Kutsumanimi";
    private final static String SUKUNIMI = "Sukunimi";
    private final static String ASIOINTIKIELI = "asiointikieli";
    private final static String LUPAJULKAISUUN = "lupaJulkaisu";
    private final static String HETU = "Henkilotunnus";
    private final static String SAHKOPOSTI = "Sähköposti";
    private final static String SYNTYMAAIKA = "syntymaaika";
    private final static String KANSALLINEN_ID = "kansallinenIdTunnus";
    private final static String PASSINNUMERO = "passinnumero";
    private final static String KANSALAISUUS = "kansalaisuus";
    private final static String POSTINUMERO_ULKOMAA = "postinumeroUlkomaa";
    private final static String KAUPUNKI_ULKOMAA = "kaupunkiUlkomaa";
    private final static String ASUINMAA = "asuinmaa";
    private final static String SUOMALAINEN_LAHIOSOITE = "lahiosoite";
    private final static String SUOMALAINEN_POSTINUMERO = "Postinumero";
    private final static String OSOITE_ULKOMAA = "osoiteUlkomaa";
    private final static String SUKUPUOLI = "sukupuoli";
    private final static String AIDINKIELI = "aidinkieli";
    private final static String NAINEN = "2";
    private final static String MIES = "1";
    private Yhteystiedot yhteystiedot = null;

    public HakemusWrapper(Hakemus hakemus) {
        if (hakemus == null) {
            this.henkilotiedot = Collections.emptyMap();
            this.lisatiedot = Collections.emptyMap();
            this.hakutoiveet = Collections.emptyMap();
        }
        this.hakemus = hakemus;
    }
    public String getUlkomainenLahiosoite() {
        getHenkilotiedot();
        return Optional.ofNullable(henkilotiedot.get(OSOITE_ULKOMAA)).orElse(
                StringUtils.EMPTY);
    }
    public String getSukupuoli() {
        getHenkilotiedot();
        return Stream.of(Optional.ofNullable(henkilotiedot.get(SUKUPUOLI)).orElse(
                StringUtils.EMPTY)).map(s -> {
            if(NAINEN.equals(s)) {
                return "Nainen";
            } else if(MIES.equals(s)) {
                return "Mies";
            }
            return s;
        }).findAny().get();
    }
    public String getSukupuoliAsIs() {
        getHenkilotiedot();
        return Optional.ofNullable(henkilotiedot.get(SUKUPUOLI)).orElse(
                StringUtils.EMPTY);
    }
    public String getAidinkieli() {
        getHenkilotiedot();
        return Optional.ofNullable(henkilotiedot.get(AIDINKIELI)).orElse(
                StringUtils.EMPTY);
    }
    public String getKaupunkiUlkomaa() {
        getHenkilotiedot();
        return Optional.ofNullable(henkilotiedot.get(KAUPUNKI_ULKOMAA)).orElse(
                StringUtils.EMPTY);
    }
    public String getUlkomainenPostinumero() {
        getHenkilotiedot();
        return Optional.ofNullable(henkilotiedot.get(POSTINUMERO_ULKOMAA)).orElse(
                StringUtils.EMPTY);
    }
    public String getSuomalainenLahiosoite() {
        getHenkilotiedot();
        return Optional.ofNullable(henkilotiedot.get(SUOMALAINEN_LAHIOSOITE)).orElse(
                StringUtils.EMPTY);
    }
    public String getSuomalainenPostinumero() {
        getHenkilotiedot();
        return Optional.ofNullable(henkilotiedot.get(SUOMALAINEN_POSTINUMERO)).orElse(
                StringUtils.EMPTY);
    }

    public Osoite getOsoite() {
        return OsoiteHakemukseltaUtil.osoiteHakemuksesta(hakemus, null, null);
    }

    public String getAsuinmaa() {
        getHenkilotiedot();
        return Optional.ofNullable(henkilotiedot.get(ASUINMAA)).orElse(
                StringUtils.EMPTY);
    }


    public String getKansallinenId() {
        getHenkilotiedot();
        return Optional.ofNullable(henkilotiedot.get(KANSALLINEN_ID)).orElse(
                StringUtils.EMPTY);
    }

    public String getKansalaisuus() {
        getHenkilotiedot();
        return Optional.ofNullable(henkilotiedot.get(KANSALAISUUS)).orElse(
                StringUtils.EMPTY);
    }

    public String getPassinnumero() {
        getHenkilotiedot();
        return Optional.ofNullable(henkilotiedot.get(PASSINNUMERO)).orElse(
                StringUtils.EMPTY);
    }

    public String getPuhelinnumero() {
        if (yhteystiedot == null) {
            this.yhteystiedot = Yhteystiedot
                    .yhteystiedotHakemukselta(hakemus);
        }
        return yhteystiedot.getPuhelinnumerotAsString();
    }

    public String getSahkopostiOsoite() {
        getHenkilotiedot();
        return Optional.ofNullable(henkilotiedot.get(SAHKOPOSTI)).orElse(
                StringUtils.EMPTY);
    }

    public String getHenkilotunnusTaiSyntymaaika() {
        getHenkilotiedot();
        return Optional.ofNullable(henkilotiedot.get(HETU)).orElse(
                Optional.ofNullable(henkilotiedot.get(SYNTYMAAIKA)).orElse(
                        StringUtils.EMPTY));
    }

    public String getSyntymaaika() {
        getHenkilotiedot();
        return Optional.ofNullable(henkilotiedot.get(SYNTYMAAIKA)).orElse(
                StringUtils.EMPTY);
    }

    public String getHenkilotunnus() {
        getHenkilotiedot();
        return Optional.ofNullable(henkilotiedot.get(HETU)).orElse(
                StringUtils.EMPTY);
    }

    public String getPersonOid() {
        if(hakemus == null) {
            return null;
        }
        return hakemus.getPersonOid();
    }

    public Integer getHakutoiveenPrioriteetti(String hakukohdeOid) {
        getHakutoiveet();

        if (hakutoiveet.containsValue(hakukohdeOid)) {
            for (Entry<String, String> s : hakutoiveet.entrySet()) {
                if (hakukohdeOid.equals(s.getValue())) {
                    String value = s.getKey().split("preference")[1].split("-")[0];
                    return NumberUtils.isNumber(value) ? NumberUtils.toInt(value) : null;
                }
            }
        }
        return null;
    }

    public String getEtunimi() {
        getHenkilotiedot(); // lazy load henkilotiedot
        if (henkilotiedot.containsKey(KUTSUMANIMI)) {
            return henkilotiedot.get(KUTSUMANIMI);
        } else if (henkilotiedot.containsKey(ETUNIMET)) {
            return henkilotiedot.get(ETUNIMET);
        } else {
            return StringUtils.EMPTY;
        }
    }

    public String getSukunimi() {
        getHenkilotiedot(); // lazy load henkilotiedot
        if (henkilotiedot.containsKey(SUKUNIMI)) {
            return henkilotiedot.get(SUKUNIMI);
        } else {
            return StringUtils.EMPTY;
        }
    }

    public boolean getLupaJulkaisuun() {
        getLisatiedot(); // lazy load henkilotiedot
        if (lisatiedot.containsKey(LUPAJULKAISUUN)) {
            String l = lisatiedot.get(LUPAJULKAISUUN);
            return Boolean.TRUE.equals(Boolean.valueOf(l));
        }
        return false;
    }

    public String getAsiointikieli() {
        getLisatiedot(); // lazy load henkilotiedot
        if (lisatiedot.containsKey(ASIOINTIKIELI)) {
            return KieliUtil.normalisoiKielikoodi(lisatiedot.get(ASIOINTIKIELI));
        } else {
            return KieliUtil.SUOMI;
        }
    }

    public Map<String, String> getLisatiedot() {
        if (lisatiedot == null) {
            lisatiedot = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            lisatiedot.putAll(hakemus.getAnswers().getLisatiedot());
        }
        return lisatiedot;
    }

    public Map<String, String> getHenkilotiedot() {
        if (henkilotiedot == null) {
            henkilotiedot = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            henkilotiedot.putAll(hakemus.getAnswers().getHenkilotiedot());
        }
        return henkilotiedot;
    }

    public Map<String, String> getHakutoiveet() {
        if (hakutoiveet == null) {
            hakutoiveet = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            hakutoiveet.putAll(hakemus.getAnswers().getHakutoiveet());
        }
        return hakutoiveet;
    }
}
