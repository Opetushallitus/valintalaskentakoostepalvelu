package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static String tilaConverter(String tila) {
        if (TILAT.containsKey(tila)) {
            return TILAT.get(tila);
        } else {
            LOG.debug("Hakemuksen tila utiliteetilla ei ole konversiota enumille: {}", tila);
            return tila;
        }
    }
}
