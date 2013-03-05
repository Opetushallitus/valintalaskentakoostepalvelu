package fi.vm.sade.valinta.kooste.wsdlmock.test;

import java.util.Arrays;
import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.HakemusService;
import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.hakemus.schema.HakutoiveTyyppi;

@Component
public class MockHakemusService implements HakemusService {

    @Override
    @WebResult(name = "hakemus", targetNamespace = "")
    @RequestWrapper(localName = "haeHakemukset", targetNamespace = "http://hakemus.service.sade.vm.fi/messages", className = "fi.vm.sade.service.hakemus.messages.HaeHakemuksetTyyppi")
    @WebMethod
    @ResponseWrapper(localName = "haeHakemuksetVastaus", targetNamespace = "http://hakemus.service.sade.vm.fi/messages", className = "fi.vm.sade.service.hakemus.messages.HaeHakemuksetVastausTyyppi")
    public List<HakemusTyyppi> haeHakemukset(@WebParam(name = "hakukohdeOid", targetNamespace = "") List<String> arg0) {
        HakemusTyyppi h = new HakemusTyyppi();
        h.setHakemusOid("hfdheh");
        return Arrays.asList(h);
    }

    @Override
    @WebResult(name = "hakutoive", targetNamespace = "")
    @RequestWrapper(localName = "haeHakutoiveet", targetNamespace = "http://hakemus.service.sade.vm.fi/messages", className = "fi.vm.sade.service.hakemus.messages.HaeHakutoiveetTyyppi")
    @WebMethod
    @ResponseWrapper(localName = "haeHakutoiveetVastaus", targetNamespace = "http://hakemus.service.sade.vm.fi/messages", className = "fi.vm.sade.service.hakemus.messages.HaeHakutoiveetVastausTyyppi")
    public List<HakutoiveTyyppi> haeHakutoiveet(@WebParam(name = "hakuOid", targetNamespace = "") String arg0) {
        HakutoiveTyyppi ht = new HakutoiveTyyppi();
        ht.setHakemusOid("sdfgsdfg");
        return Arrays.asList(ht);
    }
}
