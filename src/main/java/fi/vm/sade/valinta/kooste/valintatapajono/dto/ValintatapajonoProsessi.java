package fi.vm.sade.valinta.kooste.valintatapajono.dto;

/**
 * @author Jussi Jartamo
 */
public class ValintatapajonoProsessi {
    private final String hakuOid;
    private final String hakukohdeOid;
    private final String tarjoajaOid;

    public ValintatapajonoProsessi() {
        this.hakuOid = null;
        this.hakukohdeOid = null;
        this.tarjoajaOid = null;
    }

    public ValintatapajonoProsessi(String hakuOid, String hakukohdeOid, String tarjoajaOid) {
        this.hakuOid = hakuOid;
        this.hakukohdeOid = hakukohdeOid;
        this.tarjoajaOid = tarjoajaOid;
    }

    public String getHakukohdeOid() {
        return hakukohdeOid;
    }

    public String getHakuOid() {
        return hakuOid;
    }

    public String getTarjoajaOid() {
        return tarjoajaOid;
    }
}
