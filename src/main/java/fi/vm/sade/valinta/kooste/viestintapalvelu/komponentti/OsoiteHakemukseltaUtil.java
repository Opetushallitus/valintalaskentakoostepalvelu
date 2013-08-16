package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.vm.sade.service.hakemus.schema.AvainArvoTyyppi;
import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;

/**
 * 
 * @author Jussi Jartamo
 * 
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

    public static Osoitteet osoitteetHakemuksilta(List<HakemusTyyppi> hakemukset) {
        List<Osoite> osoitteet = new ArrayList<Osoite>();
        for (HakemusTyyppi hakemus : hakemukset) {
            osoitteet.add(osoiteHakemuksesta(hakemus));
        }
        return new Osoitteet(Collections.unmodifiableList(osoitteet));
    }

    public static Osoite osoiteHakemuksesta(HakemusTyyppi hakemus) {
        Map<String, String> m = arvotMappaus(hakemus.getAvainArvo());
        String lahiosoite = null;
        String postinumero = null;
        String postitoimipaikka = null;
        String maa = null;
        String maakoodi = m.get(ASUINMAA);
        if (SUOMI.equals(maakoodi)) { // PITAISI OLLA SUOMALAINEN OSOITE
            lahiosoite = m.get(SUOMALAINEN_LAHIOSOITE);
            postinumero = m.get(SUOMALAINEN_POSTINUMERO);
            postitoimipaikka = m.get(SUOMALAINEN_POSTITOIMIPAIKKA);
            maa = "Suomi";
        } else { // OLETATAAN ULKOMAALAINEN OSOITE
            lahiosoite = m.get(ULKOMAA_LAHIOSOITE);
            postinumero = m.get(ULKOMAA_POSTINUMERO);
            postitoimipaikka = m.get(ULKOMAA_POSTITOIMIPAIKKA);

        }

        String etunimi = hakemus.getHakijanEtunimi();
        String sukunimi = hakemus.getHakijanSukunimi();
        return new Osoite(etunimi, sukunimi, lahiosoite, null, null, postinumero, postitoimipaikka, maa, null, maakoodi);
    }

    private static Map<String, String> arvotMappaus(List<AvainArvoTyyppi> arvot) {
        Map<String, String> mappaus = new HashMap<String, String>();
        for (AvainArvoTyyppi a : arvot) {
            mappaus.put(a.getAvain(), a.getArvo());
        }
        return Collections.unmodifiableMap(mappaus);
    }
}
