package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti;

import java.util.List;

import org.apache.camel.Header;
import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

/**
 * @author Jussi Jartamo
 */
@Component("suoritaLaskentaKomponentti")
public class SuoritaLaskentaKomponentti {

    // @Autowired
    // private HaeHakukohteenHakemuksetKomponentti hakukohdeProxy;
    // @Autowired
    // private HaeHakemusKomponentti hakemusProxy;

    private ValintalaskentaService valintalaskentaService;

    @Autowired
    public SuoritaLaskentaKomponentti(ValintalaskentaService valintalaskentaService) {
        this.valintalaskentaService = valintalaskentaService;
    }

    private static final Logger LOG = LoggerFactory.getLogger(SuoritaLaskentaKomponentti.class);

    public void suoritaLaskenta(
            // @Property("hakukohdeOid") String hakukohdeOid,
            @Header("hakemustyypit") List<HakemusTyyppi> hakemustyypit,
            @Property("valintaperusteet") List<ValintaperusteetTyyppi> valintaperusteet) {
        // List<SuppeaHakemus> hakemukset =
        // hakukohdeProxy.haeHakukohteenHakemukset(hakukohdeOid);
        //
        // int hCount = 0;
        // int vCount = 0;
        // if (hakemukset != null) {
        // hCount = hakemukset.size();
        // }
        // if (valintaperusteet != null) {
        // vCount = valintaperusteet.size();
        // }
        // LOG.info("Suoritetaan valintalaskenta, hakemuksia:{} ja valintaperusteita:{}",
        // new Object[] { hCount, vCount });
        //
        // List<HakemusTyyppi> hakemustyypit = new ArrayList<HakemusTyyppi>();
        // for (SuppeaHakemus hakemus : hakemukset) {
        // LOG.info("Haetaan hakemuksen {} yksityiskohtaiset tiedot",
        // hakemus.getOid());
        // Hakemus h = hakemusProxy.haeHakemus(hakemus.getOid());
        // hakemustyypit.add(Converter.hakemusToHakemusTyyppi(h));
        // }
        //
        // LOG.info("Hakemukset haettu, käynnistetään laskenta");
        //
        valintalaskentaService.laske(hakemustyypit, valintaperusteet);
    }

}
