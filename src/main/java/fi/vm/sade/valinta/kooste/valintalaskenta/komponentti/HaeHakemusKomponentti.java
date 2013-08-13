package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti;

import fi.vm.sade.service.hakemus.HakemusService;
import fi.vm.sade.service.hakemus.schema.AvainArvoTyyppi;
import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Iterator;
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
@Component("haeHakemusKomponentti")
public class HaeHakemusKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HaeHakemusKomponentti.class);

    private static final String EI_ARVOSANAA = "Ei arvosanaa";

    @Autowired
    private HakemusService hakemusService;

    public List<HakemusTyyppi> haeHakemusKomponentti(@Simple("${property.hakukohdeOid}") String hakukohdeOid) {
        LOG.info("Haetaan hakemukset laskentaa varten hakukohteelle({})", new Object[]{hakukohdeOid});

        List<HakemusTyyppi> hakemukset = hakemusService.haeHakemukset(Arrays.asList(hakukohdeOid));

        // FIXME: Poista tämä looppi, kunhan hakemuspalvelulle saadaan aikaiseksi filtteri, joka
        // poistaa hakemukselta kaikki kentät, joiden arvo on "Ei arvosanaa".
        for (HakemusTyyppi h : hakemukset) {
            Iterator<AvainArvoTyyppi> i = h.getAvainArvo().iterator();
            while (i.hasNext()) {
                AvainArvoTyyppi aa = i.next();
                if (EI_ARVOSANAA.equals(aa.getArvo())) {
                    aa.setArvo("");
                }
            }
        }

        return hakemukset;
    }

}
