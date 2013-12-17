package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import fi.vm.sade.valinta.kooste.valvomo.dto.Prosessi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ValintalaskentaProsessi extends Prosessi {

    private String hakukohdeOid;
    private Integer valinnanvaihe;

    public ValintalaskentaProsessi(String resurssi, String toiminto, String hakuOid, String hakukohdeOid,
            Integer valinnanvaihe) {
        super(resurssi, toiminto, hakuOid);
        this.hakukohdeOid = hakukohdeOid;
        this.valinnanvaihe = valinnanvaihe;
    }

    public Integer getValinnanvaihe() {
        return valinnanvaihe;
    }

    public String getHakukohdeOid() {
        return hakukohdeOid;
    }

}
