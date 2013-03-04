package fi.vm.sade.valinta.kooste.valintalaskenta;

import java.util.Arrays;
import java.util.List;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

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
@Component("haeValintaperusteetKomponentti")
public class HaeValintaperusteetKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HaeValintaperusteetKomponentti.class);

    @Autowired
    private ValintaperusteService valintaperusteService;

    public List<ValintaperusteetTyyppi> haeLahtotiedot(@Simple("${property.hakukohdeOid}") String hakukohdeOid,
            @Simple("${property.valinnanvaihe}") Integer valinnanvaihe) {
        LOG.info("Haetaan valintaperusteet laskentaa varten hakukohteelle({}) ja valinnanvaiheelle({})", new Object[] {
                hakukohdeOid, valinnanvaihe });
        HakuparametritTyyppi hakuparametri = new HakuparametritTyyppi();
        hakuparametri.setHakukohdeOid(hakukohdeOid);
        hakuparametri.setValinnanVaiheJarjestysluku(valinnanvaihe);

        List<ValintaperusteetTyyppi> valintaperusteet = valintaperusteService.haeValintaperusteet(Arrays
                .asList(hakuparametri));

        return valintaperusteet;
    }
}
