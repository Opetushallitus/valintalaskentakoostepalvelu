package fi.vm.sade.valinta.kooste.wsdlmock;

import java.util.Arrays;
import java.util.List;

import javax.jws.WebParam;

import org.springframework.stereotype.Component;

import fi.vm.sade.service.valintaperusteet.GenericFault;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.messages.PaasykoeHakukohdeTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintatapajonoTyyppi;

@Component
public class MockValintaperusteService implements ValintaperusteService {

    @Override
    public List<ValintaperusteetTyyppi> haeValintaperusteet(List<HakuparametritTyyppi> hakuparametrit)
            throws GenericFault {
        ValintaperusteetTyyppi v = new ValintaperusteetTyyppi();
        v.setHakukohdeOid("sdfgdfg");
        v.setValinnanVaiheJarjestysluku(65);
        return Arrays.asList(v);
    }

    @Override
    public List<ValintatapajonoTyyppi> haeValintatapajonotSijoittelulle(String hakukohdeOid) throws GenericFault {
        throw new UnsupportedOperationException("Mock ei implementoi tätä!");
    }

    @Override
    public List<PaasykoeHakukohdeTyyppi> haePaasykokeet(@WebParam(name = "hakuOid", targetNamespace = "") String hakuOid)
            throws GenericFault {
        return null;
    }

}
