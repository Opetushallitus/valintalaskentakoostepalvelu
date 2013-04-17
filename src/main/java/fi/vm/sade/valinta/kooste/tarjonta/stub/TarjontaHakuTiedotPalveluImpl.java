package fi.vm.sade.valinta.kooste.tarjonta.stub;

import fi.vm.sade.valinta.kooste.tarjonta.TarjontaHakuTiedotPalvelu;

public class TarjontaHakuTiedotPalveluImpl implements TarjontaHakuTiedotPalvelu {

    public String haeHakuOid() {
        throw new RuntimeException(
                "Implementoi TarjontaHakuTiedotPalvelu ajastettua (tausta-ajo cron-homma) valintalaskentaa varten!");
    }
}
