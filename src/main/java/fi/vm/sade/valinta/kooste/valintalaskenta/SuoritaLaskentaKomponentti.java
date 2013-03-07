package fi.vm.sade.valinta.kooste.valintalaskenta;

import java.util.List;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component("suoritaLaskentaKomponentti")
public class SuoritaLaskentaKomponentti {

    @Autowired
    private ValintalaskentaService valintalaskentaService;

    private static final Logger LOG = LoggerFactory.getLogger(ValintalaskentaService.class);

    public void suoritaLaskenta(
            @Simple("${property.hakemukset}") List<HakemusTyyppi> hakemukset,
            @Simple("${property.valinnanvaiheet}") List<ValintaperusteetTyyppi> valintaperusteet) {


        valintalaskentaService.laske(hakemukset, valintaperusteet);
    }

}
