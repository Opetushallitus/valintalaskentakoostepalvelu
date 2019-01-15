package fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import io.reactivex.Observable;

public interface OhjausparametritAsyncResource {
    Observable<ParametritDTO> haeHaunOhjausparametrit(String hakuOid);
}
