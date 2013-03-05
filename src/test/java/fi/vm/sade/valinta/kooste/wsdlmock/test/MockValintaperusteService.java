package fi.vm.sade.valinta.kooste.wsdlmock.test;

import java.util.Arrays;
import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

import org.springframework.stereotype.Component;

import fi.vm.sade.service.valintaperusteet.GenericFault;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintatapajonoTyyppi;

@Component
public class MockValintaperusteService implements ValintaperusteService {

    @Override
    @WebResult(name = "valintaPerusteet", targetNamespace = "")
    @RequestWrapper(localName = "haeValintaperusteet", targetNamespace = "http://valintaperusteet.service.sade.vm.fi/messages", className = "fi.vm.sade.service.valintaperusteet.messages.HaeValintaperusteetTyyppi")
    @WebMethod
    @ResponseWrapper(localName = "haeValintaperusteetVastaus", targetNamespace = "http://valintaperusteet.service.sade.vm.fi/messages", className = "fi.vm.sade.service.valintaperusteet.messages.HaeValintaperusteetVastausTyyppi")
    public List<ValintaperusteetTyyppi> haeValintaperusteet(List<HakuparametritTyyppi> hakuparametrit)
            throws GenericFault {
        ValintaperusteetTyyppi v = new ValintaperusteetTyyppi();
        v.setHakukohdeOid("sdfgdfg");
        v.setValinnanVaiheJarjestysluku(65);
        return Arrays.asList(v);
    }

    @Override
    @WebResult(name = "valintatapajonot", targetNamespace = "")
    @RequestWrapper(localName = "haeValintatapajonotSijoittelulle", targetNamespace = "http://valintaperusteet.service.sade.vm.fi/messages", className = "fi.vm.sade.service.valintaperusteet.messages.HaeValintatapajonotSijoittelulleTyyppi")
    @WebMethod
    @ResponseWrapper(localName = "haeValintatapajonotSijoittelulleVastaus", targetNamespace = "http://valintaperusteet.service.sade.vm.fi/messages", className = "fi.vm.sade.service.valintaperusteet.messages.HaeValintatapajonotSijoittelulleVastausTyyppi")
    public List<ValintatapajonoTyyppi> haeValintatapajonotSijoittelulle(String hakukohdeOid) throws GenericFault {
        throw new UnsupportedOperationException("Mock ei implementoi tätä!");
    }

    @Override
    @RequestWrapper(localName = "haePaasykokeet", targetNamespace = "http://valintaperusteet.service.sade.vm.fi/messages", className = "fi.vm.sade.service.valintaperusteet.messages.HaePaasykokeetTyyppi")
    @WebMethod
    @ResponseWrapper(localName = "haePaasykokeetVastaus", targetNamespace = "http://valintaperusteet.service.sade.vm.fi/messages", className = "fi.vm.sade.service.valintaperusteet.messages.HaePaasykokeetVastausTyyppi")
    public void haePaasykokeet() throws GenericFault {
        // TODO Auto-generated method stub

    }
}
