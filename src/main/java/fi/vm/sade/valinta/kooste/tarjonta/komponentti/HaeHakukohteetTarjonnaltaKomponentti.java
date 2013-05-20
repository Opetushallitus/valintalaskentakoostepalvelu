package fi.vm.sade.valinta.kooste.tarjonta.komponentti;

import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import org.apache.camel.language.Simple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("hakukohteetTarjonnaltaKomponentti")
public class HaeHakukohteetTarjonnaltaKomponentti {

    @Autowired
    private TarjontaPublicService tarjontaService;

    /**
     * @return hakukohteet
     */
    public List<HakukohdeTyyppi> haeHakukohteetTarjonnalta(@Simple("${property.hakuOid}") String hakuOid) {
        List<HakukohdeTyyppi> hakukohde = tarjontaService.haeTarjonta(hakuOid).getHakukohde();
        return hakukohde;
    }

}