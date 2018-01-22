package fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import rx.Observable;

public interface OhjausparametritAsyncResource {
    Observable<ParametritDTO> haeHaunOhjausparametrit(String hakuOid);
}
