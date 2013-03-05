package fi.vm.sade.valinta.kooste.wsdlmock;

import java.util.List;

import javax.jws.WebParam;

import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

@Component
public class MockValintalaskentaService implements ValintalaskentaService {

    @Override
    public String laske(@WebParam(name = "hakukohdeOid", targetNamespace = "") String hakukohdeOid,
            @WebParam(name = "valinnanVaihe", targetNamespace = "") int valinnanVaihe,
            @WebParam(name = "hakemus", targetNamespace = "") List<HakemusTyyppi> hakemus,
            @WebParam(name = "valintaperusteet", targetNamespace = "") List<ValintaperusteetTyyppi> valintaperusteet) {
        return null;
    }
}
