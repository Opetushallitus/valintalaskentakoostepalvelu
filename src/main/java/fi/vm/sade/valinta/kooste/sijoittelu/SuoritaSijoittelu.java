package fi.vm.sade.valinta.kooste.sijoittelu;

import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

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
@Component("suoritaSijoittelu")
public class SuoritaSijoittelu {

    private static final Logger LOG = LoggerFactory.getLogger(SuoritaSijoittelu.class);

    @Autowired
    private fi.vm.sade.service.valintatiedot.ValintatietoService valintatietoService;

    @Autowired
    private fi.vm.sade.service.sijoittelu.SijoitteluService valintaperusteService;

    public void haeLahtotiedot(@Simple("${property.hakuOid}") String hakuOid) {

        LOG.info("Haetaan valintatiedot haulle {}", new Object[] {hakuOid});

        valintatietoService.haeValintatiedot(hakuOid);


    }
}
