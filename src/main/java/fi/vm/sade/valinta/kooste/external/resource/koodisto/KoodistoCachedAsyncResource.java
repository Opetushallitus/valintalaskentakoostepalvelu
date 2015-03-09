package fi.vm.sade.valinta.kooste.external.resource.koodisto;

import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Jussi Jartamo
 *
 * Wraps KoodistoAsyncResource with 1hour cache
 */
@Service
public class KoodistoCachedAsyncResource {

    private final KoodistoAsyncResource koodistoAsyncResource;

    @Autowired
    public KoodistoCachedAsyncResource(KoodistoAsyncResource koodistoAsyncResource) {
        this.koodistoAsyncResource = koodistoAsyncResource;
    }

    public void haeKoodit(String koodistoUri) {



    }

}
