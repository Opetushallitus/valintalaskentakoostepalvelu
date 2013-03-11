package fi.vm.sade.valinta.kooste.valintalaskenta;

import java.util.Arrays;
import java.util.List;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.HakemusService;
import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Camel Bean Invocation Endpoint. Ensimmäinen argumentti on
 *         hakukohdeOid. Toinen argumentti on valinnanvaiheenjärjestysluku.
 * 
 *         Paluuarvona joukko jossa ensimmäinen argumentti on hakukohdeOid,
 *         toinen argumentti on valinnanvaihe, kolmas argumentti on hakemukset
 *         listaus ja viimeisenä argumenttina valintaperusteet listaus.
 * 
 *         Laitoin toiminnallisuuden Bean endpointteihin, koska jollain
 */
@Component("haeHakemusKomponentti")
public class HaeHakemusKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HaeHakemusKomponentti.class);

    @Autowired
    private HakemusService hakemusService;

    public List<HakemusTyyppi> haeHakemusKomponentti(@Simple("${property.hakukohdeOid}") String hakukohdeOid) {
        LOG.info("Haetaan hakemukset laskentaa varten hakukohteelle({})", new Object[] { hakukohdeOid });
        return hakemusService.haeHakemukset(Arrays.asList(hakukohdeOid));

    }

}
