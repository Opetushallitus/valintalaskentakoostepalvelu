package fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import io.reactivex.Observable;

import java.util.concurrent.CompletableFuture;

public interface OhjausparametritAsyncResource {
    CompletableFuture<ParametritDTO> haeHaunOhjausparametrit(String hakuOid);
}
