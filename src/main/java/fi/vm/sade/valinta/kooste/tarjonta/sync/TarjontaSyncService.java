package fi.vm.sade.valinta.kooste.tarjonta.sync;

/**
 * TarjontaSyncService provides an interface for syncing hakukohteet from tarjonta-service.
 */
public interface TarjontaSyncService {

    /**
     * Sync hakukohteet from tarjonta
     *
     * Fetches a set of hakuOids from tarjonta-service, which represents hakus that should be automatically
     * synchronized periodically. The hakuOids are used to import hakukohteet for every corresponding haku.
     */
    void syncHakukohteetFromTarjonta();
}
