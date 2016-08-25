package fi.vm.sade.valinta.kooste.tarjonta.sync.impl;

import static rx.observables.BlockingObservable.from;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.hakuimport.route.HakuImportRoute;
import fi.vm.sade.valinta.kooste.tarjonta.sync.TarjontaSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class TarjontaSyncServiceImpl implements TarjontaSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(TarjontaSyncServiceImpl.class);

    @Autowired
    TarjontaAsyncResource tarjontaAsyncResource;

    @Autowired(required = false)
    HakuImportRoute hakuImportAktivointiRoute;

    public void syncHakukohteetFromTarjonta() {
        Set<String> hakuOids = from(tarjontaAsyncResource.findHakuOidsForAutosyncTarjonta()).first();
        if (hakuOids != null) {
            for (String hakuOid : hakuOids) {
                LOG.info("Starting synchronization for haku: " + hakuOid);
                hakuImportAktivointiRoute.asyncAktivoiHakuImport(hakuOid);
            }
        }
        else {
            LOG.info("Found no hakuOids to sync from tarjonta-service");
        }
    }
}
