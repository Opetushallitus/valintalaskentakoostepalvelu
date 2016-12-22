package fi.vm.sade.valinta.kooste.erillishaku.util;

import com.google.gson.GsonBuilder;
import fi.vm.sade.sijoittelu.domain.HakemuksenTila;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRiviBuilder;
import fi.vm.sade.valinta.kooste.erillishaku.excel.Maksuvelvollisuus;
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
        return new ErillishakuRiviBuilder()
                .sukunimi(SUKUNIMI)
                .etunimi(ETUNIMI)
                .henkilotunnus(HETU)
                .sahkoposti(EMPTY)
                .syntymaAika(SYNTYMAAIKA)
                .sukupuoli(SUKUPUOLI)
                .personOid(EMPTY)
                .aidinkieli(AIDINKIELI)
                .hakemuksenTila(HakemuksenTila.HYVAKSYTTY.toString())
                .ehdollisestiHyvaksyttavissa(false)
                .hyvaksymiskirjeLahetetty(new Date())
                .vastaanottoTila(ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI.toString())
                .ilmoittautumisTila(IlmoittautumisTila.EI_ILMOITTAUTUNUT.toString())
                .julkaistaankoTiedot(true)
                .poistetaankoRivi(false)
                .asiointikieli("FI")
                .puhelinnumero("040123456789")
                .osoite("Esimerkkitie 2")
                .postinumero("00100")
                .postitoimipaikka("HELSINKI")
                .asuinmaa("FIN")
                .kansalaisuus("FIN")
                .kotikunta("HELSINKI")
                .toisenAsteenSuoritus(true)
                .toisenAsteenSuoritusmaa("FIN")
                .maksuvelvollisuus(Maksuvelvollisuus.NOT_REQUIRED)
                .build();
    }
    public static ErillishakuRivi viallinenRiviPuuttuvillaTunnisteilla() {
        return new ErillishakuRiviBuilder()
                .sukunimi(SUKUNIMI)
                .etunimi(ETUNIMI)
                .henkilotunnus(EMPTY)
                .sahkoposti(EMPTY)
                .syntymaAika(EMPTY)
                .sukupuoli(Sukupuoli.EI_SUKUPUOLTA)
                .personOid(EMPTY)
                .aidinkieli(EMPTY)
                .hakemuksenTila(HakemuksenTila.HYVAKSYTTY.toString())
                .ehdollisestiHyvaksyttavissa(false)
                .vastaanottoTila(ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI.toString())
                .ilmoittautumisTila(IlmoittautumisTila.EI_ILMOITTAUTUNUT.toString())
                .julkaistaankoTiedot(true)
                .poistetaankoRivi(false)
                .asiointikieli("FI")
                .puhelinnumero("040123456789")
                .osoite("Esimerkkitie 2")
                .postinumero("00100")
                .postitoimipaikka("HELSINKI")
                .asuinmaa("FIN")
                .kansalaisuus("FIN")
                .kotikunta("HELSINKI")
                .toisenAsteenSuoritus(true)
                .toisenAsteenSuoritusmaa("FIN")
                .maksuvelvollisuus(Maksuvelvollisuus.NOT_REQUIRED)
                .build();
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
