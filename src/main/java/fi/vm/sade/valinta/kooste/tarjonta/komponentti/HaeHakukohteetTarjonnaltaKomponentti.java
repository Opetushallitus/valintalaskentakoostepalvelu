package fi.vm.sade.valinta.kooste.tarjonta.komponentti;

import static fi.vm.sade.tarjonta.service.types.TarjontaTila.JULKAISTU;

import java.util.Collection;

import org.apache.camel.language.Simple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;

import javax.annotation.Resource;

@Component("hakukohteetTarjonnaltaKomponentti")
public class HaeHakukohteetTarjonnaltaKomponentti {

    @Resource(name="tarjontaServiceClientAsAdmin")
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