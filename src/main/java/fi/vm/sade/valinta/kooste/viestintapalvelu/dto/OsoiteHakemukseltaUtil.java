package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Jussi Jartamo
 */
public class OsoiteHakemukseltaUtil {

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

    public static Osoitteet osoitteetHakemuksilta(List<Hakemus> hakemukset) {
        List<Osoite> osoitteet = new ArrayList<Osoite>();
        for (Hakemus hakemus : hakemukset) {
            osoitteet.add(osoiteHakemuksesta(hakemus));
        }
        return new Osoitteet(Collections.unmodifiableList(osoitteet));
    }

    public static Osoite osoiteHakemuksesta(Hakemus hakemus) {
        String lahiosoite = "";
        String postinumero = "";
        String postitoimipaikka = "";
        String maakoodi = "";
        String maa = "";

        String etunimet = "";
        String sukunimi = "";

        if (hakemus.getAnswers() != null && hakemus.getAnswers().getHenkilotiedot() != null) {
            Map<String, String> henkilotiedot = hakemus.getAnswers().getHenkilotiedot();
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

        return new Osoite(etunimet, sukunimi, lahiosoite, null, null, postinumero, postitoimipaikka, maa, null, maakoodi);
    }
}
