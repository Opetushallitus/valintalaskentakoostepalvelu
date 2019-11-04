package fi.vm.sade.valinta.kooste.external.resource.koski;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface KoskiAsyncResource {
    CompletableFuture<Set<KoskiOppija>> findKoskiOppijat(List<String> oppijanumerot);
}
