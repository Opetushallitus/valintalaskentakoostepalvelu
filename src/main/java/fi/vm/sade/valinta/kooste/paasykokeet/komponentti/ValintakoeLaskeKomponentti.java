package fi.vm.sade.valinta.kooste.paasykokeet.komponentti;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.hakemus.schema.HakukohdeTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.valinta.kooste.paasykokeet.komponentti.proxy.HakukohteenValintaperusteetProxy;
import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Component("valintakoeLaskeKomponentti")
public class ValintakoeLaskeKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(ValintakoeLaskeKomponentti.class);

    @Autowired
    private HakukohteenValintaperusteetProxy proxy;

    @Autowired
    private ValintalaskentaService valintalaskentaService;

    public void haeLahtotiedot(@Simple("${property.hakemukset}") List<HakemusTyyppi> hakemukset) {

        LOG.info("ValintakoeLaskeKomponentti, aloitetaan valintaperusteiden hakeminen ja valintalaskennan kutsut");

        for (HakemusTyyppi hakemus : hakemukset) {
            LOG.info("Haetaan tiedot hakemukselle, hakemusoid: {} ", hakemus.getHakemusOid());

            Set<String> hakukohteet = new HashSet<String>();
            for (HakukohdeTyyppi hkt : hakemus.getHakutoive()) {
                hakukohteet.add(hkt.getHakukohdeOid());
            }
            List<ValintaperusteetTyyppi> valintaperusteet = proxy.haeValintaperusteet(hakukohteet);

            valintalaskentaService.valintakokeet(hakemus, valintaperusteet);

        }
    }
}
