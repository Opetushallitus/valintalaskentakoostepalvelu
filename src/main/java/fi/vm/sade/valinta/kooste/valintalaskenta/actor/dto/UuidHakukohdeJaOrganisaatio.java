package fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto;

/**
 * @author Jussi Jartamo
 */
public class UuidHakukohdeJaOrganisaatio {
    private final String uuid;
    private final HakukohdeJaOrganisaatio hakukohdeJaOrganisaatio;
    public UuidHakukohdeJaOrganisaatio(String uuid, HakukohdeJaOrganisaatio hakukohdeJaOrganisaatio) {
        this.uuid = uuid;
        this.hakukohdeJaOrganisaatio = hakukohdeJaOrganisaatio;
    }

    public HakukohdeJaOrganisaatio getHakukohdeJaOrganisaatio() {
        return hakukohdeJaOrganisaatio;
    }

    public String getUuid() {
        return uuid;
    }
}
