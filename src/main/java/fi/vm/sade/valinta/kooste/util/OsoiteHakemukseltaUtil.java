package fi.vm.sade.valinta.kooste.util;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.OsoiteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsoiteHakemukseltaUtil {
    private final static Logger LOG = LoggerFactory.getLogger(OsoiteHakemukseltaUtil.class);

    public final static String SUOMI = "FIN";

    public static Osoite osoiteHakemuksesta(HakemusWrapper hakemus, String maa, String postitoimipaikka, NimiPaattelyStrategy nimiPaattelyStrategy) {
        String maakoodi = hakemus.getAsuinmaa();
        if (postitoimipaikka == null) {
            postitoimipaikka = SUOMI.equalsIgnoreCase(maakoodi) ? "" : hakemus.getUlkomainenPostitoimipaikka();
        }
        String postinumero = SUOMI.equalsIgnoreCase(maakoodi) ? hakemus.getSuomalainenPostinumero() : hakemus.getUlkomainenPostinumero();
        String lahiosoite = SUOMI.equalsIgnoreCase(maakoodi) ? hakemus.getSuomalainenLahiosoite() : hakemus.getUlkomainenLahiosoite();
        maa = SUOMI.equalsIgnoreCase(maakoodi) ? "Suomi" : maa;
        String maakunta = "";
        String etunimet = hakemus.getEtunimet();
        String kutsumanimi = hakemus.getEtunimi();
        String sukunimi = hakemus.getSukunimi();
        boolean ulkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt = hakemus.ulkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt();

        String nimi = nimiPaattelyStrategy.paatteleNimi(kutsumanimi, etunimet);

        return new OsoiteBuilder()
                .setFirstName(nimi)
                .setLastName(sukunimi)
                .setAddressline(lahiosoite)
                .setPostalCode(postinumero)
                .setCity(postitoimipaikka)
                .setRegion(maakunta)
                .setCountry(maa)
                .setCountryCode(maakoodi)
                .setUlkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt(ulkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt)
                .createOsoite();
    }
}
