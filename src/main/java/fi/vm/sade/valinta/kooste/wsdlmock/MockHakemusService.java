package fi.vm.sade.valinta.kooste.wsdlmock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jws.WebParam;

import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.HakemusService;
import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.hakemus.schema.HakukohdeTyyppi;
import fi.vm.sade.service.hakemus.schema.HakutoiveTyyppi;

@Component
public class MockHakemusService implements HakemusService {

    @Override
    public List<HakemusTyyppi> haeHakemukset(@WebParam(name = "hakukohdeOid", targetNamespace = "") List<String> arg0) {
        List<HakemusTyyppi> hakemukset = new ArrayList<HakemusTyyppi>();
        for (int i = 0; i < 10; ++i) {
            HakemusTyyppi h = new HakemusTyyppi();
            h.setHakemusOid("hakemusoid" + i);
            int a = 1;
            for (String hakukohdeoid : arg0) {
                HakukohdeTyyppi hakukohde = new HakukohdeTyyppi();
                hakukohde.setHakukohdeOid(hakukohdeoid);
                hakukohde.setPrioriteetti(a);
                h.getHakukohde().add(hakukohde);
                ++a;
            }
            hakemukset.add(h);
        }

        return hakemukset;
    }

    @Override
    public List<HakutoiveTyyppi> haeHakutoiveet(@WebParam(name = "hakuOid", targetNamespace = "") String arg0) {
        HakutoiveTyyppi ht = new HakutoiveTyyppi();
        ht.setHakemusOid("sdfgsdfg");
        return Arrays.asList(ht);
    }
}
