package fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.dto;

import java.util.Optional;

public class JonoPair {
    public final Jono fromLaskenta;
    public final Jono fromValintaperusteet;

    public JonoPair(Jono fromLaskenta, Jono fromValintaperusteet) {
        this.fromLaskenta = fromLaskenta;
        this.fromValintaperusteet = fromValintaperusteet;
    }
    public boolean isMolemmissaValmisSijoiteltavaksiJaSiirretaanSijoitteluun() {
        boolean laskentaOk = fromLaskenta.isValmisSijoiteltavaksiAndSiirretaanSijoitteluun();
        boolean valintaperusteetOk = fromValintaperusteet.siirretaanSijoitteluun && fromValintaperusteet.isAktiivinen();
        return laskentaOk && valintaperusteetOk;
    }

}
