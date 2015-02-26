package fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author Jussi Jartamo
 */
public interface OhjausparametritAsyncResource {

    Peruutettava haeHaunOhjausparametrit(String hakuOid,
                                              Consumer<ParametritDTO> callback,
                                              Consumer<Throwable> failureCallback);

}
