package fi.vm.sade.valinta.kooste.wsdlmock;

import fi.vm.sade.service.hakemus.HakemusService;
import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.hakemus.schema.HakukohdeTyyppi;
import org.springframework.stereotype.Component;

import javax.jws.WebParam;
import java.util.ArrayList;
import java.util.List;

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
}
