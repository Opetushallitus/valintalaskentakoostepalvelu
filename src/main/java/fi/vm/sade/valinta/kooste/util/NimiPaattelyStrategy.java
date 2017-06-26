package fi.vm.sade.valinta.kooste.util;

import org.apache.commons.lang.StringUtils;

public class NimiPaattelyStrategy {
    public String paatteleNimi(String kutsumanimi, String etunimet, String sukunimi) {
        // VT-836
        String nimi;
        if (StringUtils.isBlank(kutsumanimi)) {
            nimi = etunimet;
        } else {
            nimi = kutsumanimi;
        }
        return nimi;
    }
}
