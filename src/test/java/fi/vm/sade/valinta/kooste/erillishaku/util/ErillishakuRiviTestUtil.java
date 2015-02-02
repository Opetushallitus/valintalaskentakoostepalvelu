package fi.vm.sade.valinta.kooste.erillishaku.util;

import fi.vm.sade.sijoittelu.domain.HakemuksenTila;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import org.apache.commons.lang.StringUtils;

import java.util.Optional;

/**
 * @author Jussi Jartamo
 */
public class ErillishakuRiviTestUtil {

    private static final String SUKUNIMI = "Sukunimi";
    private static final String ETUNIMI = "Etunimi";
    private static final String HETU = "190195-933N";
    private static final String SYNTYMAAIKA = "19.01.1995";

    public static ErillishakuRivi laillinenRivi() {
        return new ErillishakuRivi(null, SUKUNIMI,ETUNIMI,HETU,StringUtils.EMPTY, SYNTYMAAIKA,StringUtils.EMPTY, HakemuksenTila.HYVAKSYTTY.toString(), ValintatuloksenTila.VASTAANOTTANUT.toString(), IlmoittautumisTila.EI_ILMOITTAUTUNUT.toString(),true, Optional.empty());
    }
    public static ErillishakuRivi viallinenRiviPuuttuvillaTunnisteilla() {
        return new ErillishakuRivi(null, SUKUNIMI,ETUNIMI,
                // HETU, SYNTYMAAIKA, HENKILOOID => TUNNISTEET
                StringUtils.EMPTY, StringUtils.EMPTY,StringUtils.EMPTY, StringUtils.EMPTY,
                //
                HakemuksenTila.HYVAKSYTTY.toString(), ValintatuloksenTila.VASTAANOTTANUT.toString(), IlmoittautumisTila.EI_ILMOITTAUTUNUT.toString(),true, Optional.empty());
    }
}
