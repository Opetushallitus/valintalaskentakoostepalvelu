package fi.vm.sade.valinta.kooste.tarjonta.stub;

import org.springframework.beans.factory.annotation.Autowired;

import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.valinta.kooste.tarjonta.TarjontaHakuTiedotPalvelu;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class TarjontaHakuTiedotPalveluImpl implements TarjontaHakuTiedotPalvelu {

    @Autowired
    private ValintalaskentaService valintalaskentaService;

    public String haeHakuOid() {
        throw new RuntimeException(
                "Implementoi TarjontaHakuTiedotPalvelu ajastettua (tausta-ajo cron-homma) valintalaskentaa varten!");
    }
}
