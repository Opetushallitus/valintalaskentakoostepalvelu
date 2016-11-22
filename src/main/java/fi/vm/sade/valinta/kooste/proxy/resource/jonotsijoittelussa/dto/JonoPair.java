package fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.dto;

import java.util.Optional;

public class JonoPair {
    public final Optional<Jono> fromLaskenta;
    public final Optional<Jono> fromValintaperusteet;

    public JonoPair(Optional<Jono> fromLaskenta, Optional<Jono> fromValintaperusteet) {
        this.fromLaskenta = fromLaskenta;
        this.fromValintaperusteet = fromValintaperusteet;
    }

    public boolean isLaskennassaValmisSijoiteltavaksiAndSiirretaanSijoitteluun() {
        return fromLaskenta.map(j -> j.isValmisSijoiteltavaksiAndSiirretaanSijoitteluun()).orElse(false);
    }

    public boolean isAinoastaanLaskennassa() {
        return fromLaskenta.isPresent() && !fromValintaperusteet.isPresent();
    }
    public boolean isAinoastaanValintaperusteissa() {
        return !fromLaskenta.isPresent() && fromValintaperusteet.isPresent();
    }
    public boolean isValintaperusteissaSiirretaanSijoitteluun() {
        return fromValintaperusteet.map(j -> j.siirretaanSijoitteluun && j.aktiivinen.orElse(false)).orElse(false);
    }

    public boolean isMolemmissaValmisSijoiteltavaksiJaSiirretaanSijoitteluun() {
        boolean laskentaOk = fromLaskenta.map(j -> j.isValmisSijoiteltavaksiAndSiirretaanSijoitteluun()).orElse(false);
        boolean valintaperusteetOk = fromValintaperusteet.map(j -> j.siirretaanSijoitteluun && j.aktiivinen.orElse(false)).orElse(false);
        return laskentaOk && valintaperusteetOk;
    }
    public String getHakukohdeOid() {
        Optional<String> hk1 = fromValintaperusteet.map(j -> j.hakukohdeOid);
        Optional<String> hk2 = fromLaskenta.map(j -> j.hakukohdeOid);
        return hk1.orElseGet(() -> hk2.get());
    }
}
