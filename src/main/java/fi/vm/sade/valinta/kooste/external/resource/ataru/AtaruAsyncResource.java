package fi.vm.sade.valinta.kooste.external.resource.ataru;

import fi.vm.sade.valinta.kooste.util.HakemusWrapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AtaruAsyncResource {
    CompletableFuture<List<HakemusWrapper>> getApplicationsByHakukohde(String hakukohdeOid);

    CompletableFuture<List<HakemusWrapper>> getApplicationsByOids(List<String> oids);
}
