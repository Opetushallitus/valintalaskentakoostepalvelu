package fi.vm.sade.valinta.kooste.valintakokeet.komponentti;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.hakemus.schema.HakukohdeTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakemusKomponentti;
import fi.vm.sade.valinta.kooste.util.Converter;
import fi.vm.sade.valinta.kooste.valintakokeet.komponentti.proxy.HakukohteenValintaperusteetProxy;

/**
 * User: wuoti Date: 29.8.2013 Time: 15.33
 */
@Component("laskeValintakoeosallistumisetHakemukselleKomponentti")
public class LaskeValintakoeosallistumisetHakemukselleKomponentti {

    private static final Logger LOG = LoggerFactory
            .getLogger(LaskeValintakoeosallistumisetHakemukselleKomponentti.class);

    @Autowired
    private HakukohteenValintaperusteetProxy proxy;

    @Autowired
    private ValintalaskentaService valintalaskentaService;

    @Autowired
    private HaeHakemusKomponentti hakemusProxy;

    public void laske(@Property(OPH.HAKEMUSOID) String hakemusOid) {
        LOG.info("Haetaan hakemus osoitteesta {}", new Object[] { hakemusOid });
        Hakemus h = hakemusProxy.haeHakemus(hakemusOid);
        HakemusTyyppi hakemusTyyppi = Converter.hakemusToHakemusTyyppi(h);

        Set<String> hakutoiveOids = new HashSet<String>();
        for (HakukohdeTyyppi ht : hakemusTyyppi.getHakutoive()) {
            hakutoiveOids.add(ht.getHakukohdeOid());
        }

        List<ValintaperusteetTyyppi> valintaperusteet = proxy.haeValintaperusteet(hakutoiveOids);
        valintalaskentaService.valintakokeet(hakemusTyyppi, valintaperusteet);
    }
}
