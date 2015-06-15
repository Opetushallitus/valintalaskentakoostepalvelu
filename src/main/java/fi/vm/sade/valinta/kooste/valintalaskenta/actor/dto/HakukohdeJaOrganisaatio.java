package fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto;

public class HakukohdeJaOrganisaatio {
    private String hakukohdeOid;
    private String organisaatioOid;

    public HakukohdeJaOrganisaatio(String hakukohdeOid, String organisaatioOid) {
        this.hakukohdeOid = hakukohdeOid;
        this.organisaatioOid = organisaatioOid;
    }

    public String getHakukohdeOid() {
        return hakukohdeOid;
    }

    public String getOrganisaatioOid() {
        return organisaatioOid;
    }
}
