package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import org.mockito.Mockito;
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
    @Autowired
    private DokumenttiAsyncResource dokumenttiAsyncResource;
    @Autowired
    private KoodistoAsyncResource koodistoAsyncResource;
    @Autowired
    private OhjausparametritAsyncResource ohjausparametritAsyncResource;

    @PostConstruct
    public void init() {
        MOCKS = this;
    }

    public static ViestintapalveluAsyncResource getViestintapalveluAsyncResource() {
        return MOCKS.viestintapalveluAsyncResource;
    }
    public static OhjausparametritAsyncResource getOhjausparametritAsyncResource() {
        return MOCKS.ohjausparametritAsyncResource;
    }
    public static KoodistoAsyncResource getKoodistoAsyncResource() {
        return MOCKS.koodistoAsyncResource;
    }
    public static HakukohdeResource getHakukohdeResource() {
        return MOCKS.hakukohdeResource;
    }
    public static DokumenttiAsyncResource getDokumenttiAsyncResource() {
        return MOCKS.dokumenttiAsyncResource;
    }

    public static void reset() {
        Mockito.reset(getViestintapalveluAsyncResource(), getKoodistoAsyncResource(), getHakukohdeResource(), getDokumenttiAsyncResource());
    }
}
