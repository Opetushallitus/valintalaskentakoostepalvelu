package fi.vm.sade.valinta.kooste.kela.dto;

import fi.vm.sade.valinta.kooste.valvomo.dto.Prosessi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class KelaProsessi extends Prosessi {

    /**
     * Optional
     */
    private String dokumenttiId;

    public KelaProsessi(String toiminto, String hakuOid, String dokumenttiId) {
        super("Kela", toiminto, hakuOid);
        this.dokumenttiId = dokumenttiId;
    }

    public String getDokumenttiId() {
        return dokumenttiId;
    }

}
