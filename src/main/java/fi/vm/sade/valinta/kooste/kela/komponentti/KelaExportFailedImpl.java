package fi.vm.sade.valinta.kooste.kela.komponentti;

import fi.vm.sade.valinta.kooste.kela.KelaCache;
import fi.vm.sade.valinta.kooste.kela.dto.KelaCacheDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("failedKelaExport")
public class KelaExportFailedImpl {

    private static final Logger LOG = LoggerFactory.getLogger(KelaExportFailedImpl.class);

    @Autowired
    private KelaCache kelaCache;

    public void failedKelaExport() {
        LOG.error("Kela export epäonnistui!");
        kelaCache.addDocument(KelaCacheDocument.createErrorMessage("Kela-tiedoston luonti epäonnistui!"));
    }
}
