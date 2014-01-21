package fi.vm.sade.valinta.kooste.external.resource.haku.dto;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * User: wuoti
 * Date: 3.9.2013
 * Time: 14.40
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KoodistoUrheilija {

    private String koodiUri;

    public String getKoodiUri() {
        return koodiUri;
    }

    public void setKoodiUri(String koodiUri) {
        this.koodiUri = koodiUri;
    }
}
