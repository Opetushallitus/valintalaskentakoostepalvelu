package fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.dto;

import java.util.Optional;

public class Jono {
    public final String oid;
    public final String hakukohdeOid;
    public final Optional<Boolean> valmisSijoiteltavaksi;
    public final boolean siirretaanSijoitteluun;
    public Jono(String hakukohdeOid, String oid, Optional<Boolean> valmisSijoiteltavaksi, boolean siirretaanSijoitteluun) {
        this.oid = oid;
        this.hakukohdeOid = hakukohdeOid;
        this.valmisSijoiteltavaksi = valmisSijoiteltavaksi;
        this.siirretaanSijoitteluun = siirretaanSijoitteluun;
    }

    public boolean isValmisSijoiteltavaksiAndSiirretaanSijoitteluun() {
        return valmisSijoiteltavaksi.orElse(false) && siirretaanSijoitteluun;
    }
}
