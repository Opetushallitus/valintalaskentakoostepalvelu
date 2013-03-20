package fi.vm.sade.valinta.kooste.tarjonta;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.language.Simple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import fi.vm.sade.tarjonta.service.types.TarjontaTyyppi;

@Component("hakukohteetTarjonnaltaKomponentti")
public class HaeHakukohteetTarjonnaltaKomponentti {

    @Autowired
    private TarjontaPublicService tarjontaService;

    public List<String> haeHakukohteetTarjonnalta(@Simple("${property.hakuoid}") String hakuoid) {
        TarjontaTyyppi tarjonta = tarjontaService.haeTarjonta(hakuoid);
        List<String> hakukohdeoidit = new ArrayList<String>();
        for (HakukohdeTyyppi hakukohde : tarjonta.getHakukohde()) {
            hakukohdeoidit.add(hakukohde.getOid());
        }
        return hakukohdeoidit;
    }

}