package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * @author Jussi Jartamo
 */
@Service
public class Mocks {

    private static Mocks MOCKS;

    @Autowired
    private ViestintapalveluAsyncResource viestintapalveluAsyncResource;
    @Autowired
    private HakukohdeResource hakukohdeResource;

    @PostConstruct
    public void init() {
        MOCKS = this;
    }

    public static ViestintapalveluAsyncResource getViestintapalveluAsyncResource() {
        return MOCKS.viestintapalveluAsyncResource;
    }

    public static HakukohdeResource getHakukohdeResource() {
        return MOCKS.hakukohdeResource;
    }
}
