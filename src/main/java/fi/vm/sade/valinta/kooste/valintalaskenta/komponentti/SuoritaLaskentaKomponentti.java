package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;
import fi.vm.sade.valinta.kooste.util.Converter;
import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jussi Jartamo
 */
@Component("suoritaLaskentaKomponentti")
public class SuoritaLaskentaKomponentti {

    @Autowired
    private ValintalaskentaService valintalaskentaService;

    @Autowired
    private ApplicationResource applicationResource;

    private static final Logger LOG = LoggerFactory.getLogger(SuoritaLaskentaKomponentti.class);

    public void suoritaLaskenta(@Simple("${property.hakemukset}") List<SuppeaHakemus> hakemukset,
                                @Simple("${property.valintaperusteet}") List<ValintaperusteetTyyppi> valintaperusteet) {
        assert (SecurityContextHolder.getContext().getAuthentication() != null);
        int hCount = 0;
        int vCount = 0;
        if (hakemukset != null) {
            hCount = hakemukset.size();
        }
        if (valintaperusteet != null) {
            vCount = valintaperusteet.size();
        }
        LOG.info("Suoritetaan valintalaskenta, hakemuksia:{} ja valintaperusteita:{}", new Object[]{hCount, vCount});

        List<HakemusTyyppi> hakemustyypit = new ArrayList<HakemusTyyppi>();
        for (SuppeaHakemus hakemus : hakemukset) {
            LOG.info("Haetaan hakemuksen {} yksityiskohtaiset tiedot", hakemus.getOid());
            Hakemus h = applicationResource.getApplicationByOid(hakemus.getOid());
            hakemustyypit.add(Converter.hakemusToHakemusTyyppi(h));
        }

        LOG.info("Hakemukset haettu, käynnistetään laskenta");
        valintalaskentaService.laske(hakemustyypit, valintaperusteet);
    }

}
