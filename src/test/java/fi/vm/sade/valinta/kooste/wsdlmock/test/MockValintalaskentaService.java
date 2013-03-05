package fi.vm.sade.valinta.kooste.wsdlmock.test;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

@Component
public class MockValintalaskentaService implements ValintalaskentaService {

    @Override
    @WebResult(name = "status", targetNamespace = "")
    @RequestWrapper(localName = "laske", targetNamespace = "http://valintalaskenta.service.sade.vm.fi/messages", className = "fi.vm.sade.service.valintalaskenta.messages.LaskeTyyppi")
    @WebMethod
    @ResponseWrapper(localName = "laskeVastaus", targetNamespace = "http://valintalaskenta.service.sade.vm.fi/messages", className = "fi.vm.sade.service.valintalaskenta.messages.LaskeVastausTyyppi")
    public String laske(@WebParam(name = "hakukohdeOid", targetNamespace = "") String hakukohdeOid,
            @WebParam(name = "valinnanVaihe", targetNamespace = "") int valinnanVaihe,
            @WebParam(name = "hakemus", targetNamespace = "") List<HakemusTyyppi> hakemus,
            @WebParam(name = "valintaperusteet", targetNamespace = "") List<ValintaperusteetTyyppi> valintaperusteet) {
        return null;
    }
}
