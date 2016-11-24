package fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.dto;

import java.util.Optional;

public class Jono {
    public final String oid;
    public final String hakukohdeOid;
    public final Optional<Boolean> valmisSijoiteltavaksi;
    public final boolean siirretaanSijoitteluun;
    public final Optional<Boolean> aktiivinen;
    public Jono(String hakukohdeOid, String oid, Optional<Boolean> valmisSijoiteltavaksi, boolean siirretaanSijoitteluun, Optional<Boolean> aktiivinen) {
        this.oid = oid;
        this.hakukohdeOid = hakukohdeOid;
        this.aktiivinen = aktiivinen;
        this.valmisSijoiteltavaksi = valmisSijoiteltavaksi;
        this.siirretaanSijoitteluun = siirretaanSijoitteluun;
    }

    public boolean isValmisSijoiteltavaksiAndSiirretaanSijoitteluun() {
        return valmisSijoiteltavaksi.orElse(false) && siirretaanSijoitteluun;
    }

    /**
     * @return puuttuva aktiivinen tulkitaan falseksi
     */
    public boolean isPassiivinen() {
        return !(aktiivinen.orElse(true));
    }

    /**
     * @return puuttuva aktiivinen tulkitaan falseksi
     */
    public boolean isAktiivinen() {
        return aktiivinen.orElse(false);
    }
}
