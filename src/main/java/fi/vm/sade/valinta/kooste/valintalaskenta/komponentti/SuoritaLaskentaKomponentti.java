package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Property;
import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;
import fi.vm.sade.valinta.kooste.haku.HakemusProxy;
import fi.vm.sade.valinta.kooste.haku.HakukohdeProxy;
import fi.vm.sade.valinta.kooste.util.Converter;

/**
 * @author Jussi Jartamo
 */
@Component("suoritaLaskentaKomponentti")
public class SuoritaLaskentaKomponentti {

    @Autowired
    private ValintalaskentaService valintalaskentaService;

    @Autowired
    private HakukohdeProxy hakukohdeProxy;
    @Autowired
    private HakemusProxy hakemusProxy;

    private static final Logger LOG = LoggerFactory.getLogger(SuoritaLaskentaKomponentti.class);

    public void suoritaLaskenta(@Property("hakukohdeOid") String hakukohdeOid,
            @Simple("${property.valintaperusteet}") List<ValintaperusteetTyyppi> valintaperusteet) {
        List<SuppeaHakemus> hakemukset = hakukohdeProxy.haeHakukohteenHakemukset(hakukohdeOid);

        int hCount = 0;
        int vCount = 0;
        if (hakemukset != null) {
            hCount = hakemukset.size();
        }
        if (valintaperusteet != null) {
            vCount = valintaperusteet.size();
        }
        LOG.info("Suoritetaan valintalaskenta, hakemuksia:{} ja valintaperusteita:{}", new Object[] { hCount, vCount });

        List<HakemusTyyppi> hakemustyypit = new ArrayList<HakemusTyyppi>();
        for (SuppeaHakemus hakemus : hakemukset) {
            LOG.info("Haetaan hakemuksen {} yksityiskohtaiset tiedot", hakemus.getOid());
            Hakemus h = hakemusProxy.haeHakemus(hakemus.getOid());
            hakemustyypit.add(Converter.hakemusToHakemusTyyppi(h));
        }

        LOG.info("Hakemukset haettu, käynnistetään laskenta");
        valintalaskentaService.laske(hakemustyypit, valintaperusteet);
    }

}
