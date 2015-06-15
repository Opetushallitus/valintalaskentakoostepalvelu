package fi.vm.sade.valinta.kooste.sijoitteluntulos.dto;

public class Valmis {
    private final String tarjoajaOid;
    private final String tulosId;
    private final String hakukohdeOid;
    private final boolean eiTuloksia;
    private final Tiedosto tiedosto;

    public Valmis(String hakukohdeOid, String tarjoajaOid, String tulosId) {
        this.hakukohdeOid = hakukohdeOid;
        this.tarjoajaOid = tarjoajaOid;
        this.tulosId = tulosId;
        this.eiTuloksia = false;
        this.tiedosto = null;
    }

    public Valmis(Tiedosto tiedosto, String hakukohdeOid, String tarjoajaOid) {
        this.hakukohdeOid = hakukohdeOid;
        this.tarjoajaOid = tarjoajaOid;
        this.tulosId = null;
        this.eiTuloksia = false;
        this.tiedosto = tiedosto;
    }

    public Valmis(String hakukohdeOid, String tarjoajaOid, String tulosId, boolean eiTuloksia) {
        this.hakukohdeOid = hakukohdeOid;
        this.tarjoajaOid = tarjoajaOid;
        this.tulosId = tulosId;
        this.eiTuloksia = eiTuloksia;
        this.tiedosto = null;
    }

    public boolean isEiTuloksia() {
        return eiTuloksia;
    }

    public boolean containsTiedosto() {
        return tiedosto != null;
    }

    public boolean isOnnistunut() {
        return tulosId != null || tiedosto != null;
    }

    public String getHakukohdeOid() {
        return hakukohdeOid;
    }

    public String getTarjoajaOid() {
        return tarjoajaOid;
    }

    public String getTulosId() {
        return tulosId;
    }

    public Tiedosto getTiedosto() {
        return tiedosto;
    }

}
