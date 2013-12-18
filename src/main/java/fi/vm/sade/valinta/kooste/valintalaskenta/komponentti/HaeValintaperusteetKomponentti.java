package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti;

import java.util.List;

import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.valintakokeet.komponentti.proxy.HakukohteenValintaperusteetProxy;
import fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.proxy.ValinnanVaiheenValintaperusteetProxy;

/**
 * @author Jussi Jartamo
 *         <p/>
 *         Camel Bean Invocation Endpoint. Ensimmäinen argumentti on
 *         hakukohdeOid. Toinen argumentti on valinnanvaiheenjärjestysluku.
 *         <p/>
 *         Paluuarvona joukko jossa ensimmäinen argumentti on hakukohdeOid,
 *         toinen argumentti on valinnanvaihe, kolmas argumentti on hakemukset
 *         listaus ja viimeisenä argumenttina valintaperusteet listaus.
 *         <p/>
 */
@Component("haeValintaperusteetKomponentti")
public class HaeValintaperusteetKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HaeValintaperusteetKomponentti.class);

    private ValinnanVaiheenValintaperusteetProxy valinnanVaiheenValintaperusteetProxy;
    private HakukohteenValintaperusteetProxy hakukohteenValintaperusteetProxy;

    @Autowired
    public HaeValintaperusteetKomponentti(HakukohteenValintaperusteetProxy hakukohteenValintaperusteetProxy,
            ValinnanVaiheenValintaperusteetProxy valinnanVaiheenValintaperusteetProxy) {
        this.valinnanVaiheenValintaperusteetProxy = valinnanVaiheenValintaperusteetProxy;
        this.hakukohteenValintaperusteetProxy = hakukohteenValintaperusteetProxy;
    }

    public List<ValintaperusteetTyyppi> haeLahtotiedot(@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
            @Property(OPH.VALINNANVAIHE) Integer valinnanvaihe) {
        LOG.info("Haetaan valintaperusteet laskentaa varten hakukohteelle({}) ja valinnanvaiheelle({})", new Object[] {
                hakukohdeOid, valinnanvaihe });

        List<ValintaperusteetTyyppi> valintaperusteet = null;
        if (valinnanvaihe == null) {
            valintaperusteet = hakukohteenValintaperusteetProxy.haeValintaperusteet(hakukohdeOid);
        } else {
            valintaperusteet = valinnanVaiheenValintaperusteetProxy.haeValintaperusteet(hakukohdeOid, valinnanvaihe);
        }

        LOG.info("valintaperusteet haettu: " + valintaperusteet.size());

        return valintaperusteet;
    }
}
