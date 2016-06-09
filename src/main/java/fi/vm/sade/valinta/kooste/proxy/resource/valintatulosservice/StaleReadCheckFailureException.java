package fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice;

import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.HakukohteenValintatulosUpdateStatuses;

public class StaleReadCheckFailureException extends RuntimeException {
    public final HakukohteenValintatulosUpdateStatuses updateStatuses;

    public StaleReadCheckFailureException(HakukohteenValintatulosUpdateStatuses updateStatuses) {
        this.updateStatuses = updateStatuses;
    }
}
