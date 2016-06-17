package fi.vm.sade.valinta.kooste.erillishaku.util;

import com.google.gson.GsonBuilder;
import fi.vm.sade.sijoittelu.domain.HakemuksenTila;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.Sukupuoli;

import java.util.Date;

import static org.apache.commons.lang.StringUtils.EMPTY;

/**
 * @author Jussi Jartamo
 */
public class ErillishakuRiviTestUtil {

    private static final String SUKUNIMI = "Sukunimi";
    private static final String ETUNIMI = "Etunimi";
    private static final String HETU = "190195-933N";
    private static final String SYNTYMAAIKA = "19.01.1995";
    private static final Sukupuoli SUKUPUOLI = Sukupuoli.NAINEN;
    private static final String AIDINKIELI = "FI";

    public static ErillishakuRivi laillinenRivi() {
        return new ErillishakuRivi(null, SUKUNIMI, ETUNIMI, HETU,
                EMPTY, SYNTYMAAIKA, SUKUPUOLI, EMPTY,
                AIDINKIELI, HakemuksenTila.HYVAKSYTTY.toString(), false, new Date(),
                ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI.toString(),
                IlmoittautumisTila.EI_ILMOITTAUTUNUT.toString(), true, false, "FI",
                "040123456789", "Esimerkkitie 2", "00100", "HELSINKI", "FIN", "FIN", "HELSINKI", "FIN");
    }
    public static ErillishakuRivi viallinenRiviPuuttuvillaTunnisteilla() {
        return new ErillishakuRivi(null, SUKUNIMI, ETUNIMI,
                EMPTY, EMPTY, EMPTY, Sukupuoli.EI_SUKUPUOLTA, EMPTY, EMPTY, HakemuksenTila.HYVAKSYTTY.toString(), false, null,
                ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI.toString(),
                IlmoittautumisTila.EI_ILMOITTAUTUNUT.toString(), true, false, "FI",
                "040123456789", "Esimerkkitie 2", "00100", "HELSINKI", "FIN", "FIN", "HELSINKI", "FIN");
    }

    public static String viallinenJsonRiviPuuttuvillaTunnisteilla() {

        return "{\"rivit\":["+new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create().toJson(laillinenRivi())+ ","+

                "{\"etunimi\":\"Etunimi\",\"sukunimi\":\"Sukunimi\"," +
                "\"henkilotunnus\":\"\",\"sahkoposti\":\"\",\"syntymaAika\"" +
                ":\"\",\"sukupuoli\":\"2\",\"aidinkieli\":\"\",\"personOid\"" +
                ":\"\",\"hakemuksenTila\":\"HYVAKSYTTY\", \"ehdollisestiHyvaksytty\":\"false\", \"vastaanottoTila\"" +
                ":\"VASTAANOTTANUT_SITOVASTI\",\"ilmoittautumisTila\"" +
                ":\"EI_ILMOITTAUTUNUT\",\"julkaistaankoTiedot\":true,\"poistetaankoRivi\":false}"
                +"]}";
    }
}
