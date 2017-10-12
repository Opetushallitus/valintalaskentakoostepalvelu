package fi.vm.sade.valinta.kooste.valintalaskenta.route;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaStartParams;

public interface ValintalaskentaKerrallaRoute {
    void suoritaValintalaskentaKerralla(final HakuV1RDTO haku, final ParametritDTO parametritDTO, LaskentaStartParams laskentaStartParams);

    void workAvailable();
}
