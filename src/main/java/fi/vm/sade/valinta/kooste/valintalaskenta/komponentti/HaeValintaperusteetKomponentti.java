package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti;

import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.valinta.kooste.paasykokeet.komponentti.proxy.HakukohteenValintaperusteetProxy;
import fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.proxy.ValinnanVaiheenValintaperusteetProxy;
import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

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
 *         Laitoin toiminnallisuuden Bean endpointteihin, koska jollain
 */
@Component("haeValintaperusteetKomponentti")
public class HaeValintaperusteetKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HaeValintaperusteetKomponentti.class);

    @Autowired
    private ValinnanVaiheenValintaperusteetProxy valinnanVaiheenValintaperusteetProxy;

    @Autowired
    private HakukohteenValintaperusteetProxy hakukohteenValintaperusteetProxy;

    public List<ValintaperusteetTyyppi> haeLahtotiedot(@Simple("${property.hakukohdeOid}") String hakukohdeOid,
                                                       @Simple("${property.valinnanvaihe}") Integer valinnanvaihe) {

        LOG.info("Haetaan valintaperusteet laskentaa varten hakukohteelle({}) ja valinnanvaiheelle({})", new Object[]{
                hakukohdeOid, valinnanvaihe});

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
