package fi.vm.sade.valinta.kooste.util;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;

/**
 * @author Jussi Jartamo
 */
public class OsoiteHakemukseltaUtil {

    private final static Logger LOG = LoggerFactory.getLogger(OsoiteHakemukseltaUtil.class);

    // KOVAKOODATTUJA ARVOJA JOTKA TULEE KOODISTOLTA MUTTA JOIDEN HANKKIMISEEN
    // EI OLE TOISTAISEKSI OLEMASSA OLEVAA KAYTANTOA!
    private final static String ASUINMAA = "asuinmaa";
    private final static String SUOMI = "FIN";
    private final static String SUOMALAINEN_LAHIOSOITE = "lahiosoite";
    private final static String SUOMALAINEN_POSTINUMERO = "Postinumero";
    private final static String SUOMALAINEN_POSTITOIMIPAIKKA = "postitoimipaikka";
    private final static String ULKOMAA_LAHIOSOITE = "osoiteUlkomaa";
    private final static String ULKOMAA_POSTINUMERO = "postinumeroUlkomaa";
    private final static String ULKOMAA_POSTITOIMIPAIKKA = "kaupunkiUlkomaa";
    private final static String ETUNIMET = "Etunimet";
    private final static String SUKUNIMI = "Sukunimi";

    public static Osoite osoiteHakemuksesta(Hakemus hakemus) {
        String lahiosoite = "";
        String postinumero = "";
        String postitoimipaikka = "";
        String maakoodi = "";
        String maa = "";

        String etunimet = "";
        String sukunimi = "";
        if (hakemus != null) {
            if (hakemus.getAnswers() != null && hakemus.getAnswers().getHenkilotiedot() != null) {
                Map<String, String> henkilotiedot = hakemus.getAnswers().getHenkilotiedot();
                if (henkilotiedot == null) {
                    LOG.error("Hakemuksella {} ei ole henkilötietoja!", hakemus.getOid());
                }
                maakoodi = henkilotiedot.get(ASUINMAA);
                etunimet = henkilotiedot.get(ETUNIMET);
                sukunimi = henkilotiedot.get(SUKUNIMI);

                if (SUOMI.equals(maakoodi)) { // PITAISI OLLA SUOMALAINEN OSOITE
                    lahiosoite = henkilotiedot.get(SUOMALAINEN_LAHIOSOITE);
                    postinumero = henkilotiedot.get(SUOMALAINEN_POSTINUMERO);
                    postitoimipaikka = henkilotiedot.get(SUOMALAINEN_POSTITOIMIPAIKKA);
                    maa = "Suomi";
                } else { // OLETATAAN ULKOMAALAINEN OSOITE
                    lahiosoite = henkilotiedot.get(ULKOMAA_LAHIOSOITE);
                    postinumero = henkilotiedot.get(ULKOMAA_POSTINUMERO);
                    postitoimipaikka = henkilotiedot.get(ULKOMAA_POSTITOIMIPAIKKA);
                }
            }
        } else {
            etunimet = "Hakemus ei ole olemassa!";
            sukunimi = "Hakemus ei ole olemassa!";
            LOG.error("Null-hakemukselle yritetään luoda osoitetta!");
        }
        return new Osoite(etunimet, sukunimi, lahiosoite, null, null, postinumero, postitoimipaikka, maa, null,
                maakoodi);
    }
}
