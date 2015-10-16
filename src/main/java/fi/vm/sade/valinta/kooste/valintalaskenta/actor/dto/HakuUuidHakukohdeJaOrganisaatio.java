package fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;

public class HakuUuidHakukohdeJaOrganisaatio extends UuidHakukohdeJaOrganisaatio {
    private final HakuV1RDTO haku;

    public HakuUuidHakukohdeJaOrganisaatio(final HakuV1RDTO haku, final String uuid, final HakukohdeJaOrganisaatio hakukohdeJaOrganisaatio) {
        super(uuid, hakukohdeJaOrganisaatio);
        this.haku = haku;
    }

    public HakuV1RDTO getHaku() {
        return haku;
    }
}
