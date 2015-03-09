package fi.vm.sade.valinta.kooste.external.resource.koodisto;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author Jussi Jartamo
 */
public interface KoodistoAsyncResource {

    Peruutettava haeKoodisto(String koodistoUri,
                                   Consumer<List<Koodi>> callback, Consumer<Throwable> failureCallback);

}
