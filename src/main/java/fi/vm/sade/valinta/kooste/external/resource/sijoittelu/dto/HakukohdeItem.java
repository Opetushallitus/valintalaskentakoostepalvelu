package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto;

import java.io.Serializable;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Koska sijoittelulla ei ole omaa API:a!
 */
public class HakukohdeItem implements Serializable {

    private String id;
    private String oid;

    public String getId() {
        return id;
    }

    public String getOid() {
        return oid;
    }

}