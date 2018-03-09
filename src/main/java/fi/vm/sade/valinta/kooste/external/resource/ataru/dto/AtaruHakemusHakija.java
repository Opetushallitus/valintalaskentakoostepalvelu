package fi.vm.sade.valinta.kooste.external.resource.ataru.dto;

public class AtaruHakemusHakija {

    private AtaruHakemus hakemus;
    private String opiskelijaOid;

    public void setOpiskelijaOid(String opiskelijaOid) {
        this.opiskelijaOid = opiskelijaOid;
    }

    public void setHakemus(AtaruHakemus hakemus) {
        this.hakemus = hakemus;
    }

    public String getOpiskelijaOid() {
        return opiskelijaOid;
    }

    public AtaruHakemus getHakemus() {
        return hakemus;
    }
}
