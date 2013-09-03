package fi.vm.sade.valinta.kooste.rest.haku.dto;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * User: wuoti
 * Date: 3.9.2013
 * Time: 14.40
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SuppeaHakemus {
    private String oid;

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }
}
