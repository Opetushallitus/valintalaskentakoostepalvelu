package fi.vm.sade.valinta.kooste.tarjonta.komponentti;

import static fi.vm.sade.tarjonta.service.types.TarjontaTila.JULKAISTU;

import java.util.Collection;

import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import fi.vm.sade.valinta.kooste.OPH;

@Component("hakukohteetTarjonnaltaKomponentti")
public class HaeHakukohteetTarjonnaltaKomponentti {

    private TarjontaPublicService tarjontaService;

    @Autowired
    public HaeHakukohteetTarjonnaltaKomponentti(TarjontaPublicService tarjontaService) {
        this.tarjontaService = tarjontaService;
    }

    public Collection<HakukohdeTyyppi> haeHakukohteetTarjonnalta(@Property(OPH.HAKUOID) String hakuOid) {
        return Collections2.filter(tarjontaService.haeTarjonta(hakuOid).getHakukohde(),
                new Predicate<HakukohdeTyyppi>() {
                    public boolean apply(HakukohdeTyyppi hakukohde) {
                        return JULKAISTU == hakukohde.getHakukohteenTila();
                    }
                });
    }
}