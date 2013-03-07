package fi.vm.sade.valinta.kooste.paasykokeet;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.hakemus.schema.HakukohdeTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;




@Component("valintakoeLaskeKomponentti")
public class ValintakoeLaskeKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(ValintakoeLaskeKomponentti.class);

    @Autowired
    private ValintaperusteService valintaperusteService;

    @Autowired
    private ValintalaskentaService valintalaskentaService;

    public void haeLahtotiedot( @Simple("${property.hakemukset}") List<HakemusTyyppi> hakemukset) {

         LOG.info("ValintakoeLaskeKomponentti, aloitetaan valintaperusteiden hakeminen ja valintalaskennan kutsut");

        for(HakemusTyyppi hakemus : hakemukset) {
            LOG.info("Haetaan tiedot hakemukselle, hakemusoid: {} ", hakemus.getHakemusOid() );

            List <HakuparametritTyyppi> hptl = new ArrayList<HakuparametritTyyppi>();
            for(HakukohdeTyyppi hkt : hakemus.getHakutoive())        {
                String oid =      hkt.getHakukohdeOid();
                HakuparametritTyyppi ht = new HakuparametritTyyppi();
                ht.setHakukohdeOid(oid);
                hptl.add(ht);
            }
            List<ValintaperusteetTyyppi> valintaperusteet = valintaperusteService.haeValintaperusteet(hptl);

            valintalaskentaService.valintakokeet(hakemus, valintaperusteet);

        }
    }
}
