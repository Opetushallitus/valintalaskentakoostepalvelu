package fi.vm.sade.valinta.kooste.tarjonta.komponentti;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import org.apache.camel.language.Simple;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collection;

import static fi.vm.sade.tarjonta.service.types.TarjontaTila.JULKAISTU;

@Component("hakukohteetTarjonnaltaKomponentti")
public class HaeHakukohteetTarjonnaltaKomponentti {

    @Resource
    private TarjontaPublicService tarjontaService;

    /**
     * @return hakukohteet
     */
    public Collection<HakukohdeTyyppi> haeHakukohteetTarjonnalta(@Simple("${property.hakuOid}") String hakuOid) {
        return Collections2.filter(tarjontaService.haeTarjonta(hakuOid).getHakukohde(),
                new Predicate<HakukohdeTyyppi>() {
                    public boolean apply(HakukohdeTyyppi hakukohde) {
                        return JULKAISTU == hakukohde.getHakukohteenTila();
                    }
                });
    }

}