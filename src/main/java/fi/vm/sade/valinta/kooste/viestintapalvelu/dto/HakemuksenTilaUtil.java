package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;

public class HakemuksenTilaUtil {

    private static final Logger LOG = LoggerFactory.getLogger(HakemuksenTilaUtil.class);
    private final static Map<String, String> TILAT = valmistaTilat();

    private static Map<String, String> valmistaTilat() {
        Map<String, String> tmp = new HashMap<String, String>();
        tmp.put("HYLATTY", "Hylätty");
        tmp.put("VARALLA", "Varalla");
        tmp.put("PERUUNTUNUT", "Peruuntunut");
        tmp.put("HYVAKSYTTY", "Hyväksytty");
        tmp.put("PERUNUT", "Perunut");
        return Collections.unmodifiableMap(tmp);
    }

    public static String tilaConverter(HakemuksenTila tila, boolean harkinnanvarainen) {
        switch (tila) {
        case HYLATTY:
            return "Hylätty";
        case VARALLA:
            return "Varalla";
        case PERUUNTUNUT:
            return "Peruuntunut";
        case HYVAKSYTTY:
            if (harkinnanvarainen) {
                return "Harkinnanvaraisesti hyväksytty";
            }
            return "Hyväksytty";
        case PERUNUT:
            return "Perunut";
        default:
            LOG.error("Hakemuksen tila utiliteetilla ei ole konversiota enumille: {}", tila);
            return tila.toString();
        }
    }
}
