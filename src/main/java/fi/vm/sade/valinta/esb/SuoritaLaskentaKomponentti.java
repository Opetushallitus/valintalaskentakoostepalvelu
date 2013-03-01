package fi.vm.sade.valinta.esb;

import java.util.List;

import org.apache.camel.Header;
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
@Component("SuoritaLaskentaKomponentti")
public class SuoritaLaskentaKomponentti {

    @Autowired
    ValintalaskentaService valintalaskentaService;

    public void suoritaLaskenta(@Header("hakukohdeOid") String hakukohdeOid,
            @Header("valinnanvaihe") Integer valinnanVaihe, @Header("hakemukset") List<HakemusTyyppi> hakemukset,
            @Header("valintaperusteet") List<ValintaperusteetTyyppi> valinnanvaiheet) {
        valintalaskentaService.laske(hakukohdeOid, valinnanVaihe, hakemukset, valinnanvaiheet);
    }

}
