package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Hakukohde implements Serializable {

    private String id;
    private Long sijoitteluajoId;
    private String oid;
    private HakukohdeTila tila;

    private String tarjoajaOid;
    private List<Valintatapajono> valintatapajonot = new ArrayList<Valintatapajono>();
    private List<Hakijaryhma> hakijaryhmat = new ArrayList<Hakijaryhma>();

    public List<Valintatapajono> getValintatapajonot() {
        return valintatapajonot;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setValintatapajonot(List<Valintatapajono> valintatapajonot) {
        this.valintatapajonot = valintatapajonot;
    }

    public List<Hakijaryhma> getHakijaryhmat() {
        return hakijaryhmat;
    }

    public void setHakijaryhmat(List<Hakijaryhma> hakijaryhmat) {
        this.hakijaryhmat = hakijaryhmat;
    }

    public HakukohdeTila getTila() {
        return tila;
    }

    public void setTila(HakukohdeTila tila) {
        this.tila = tila;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public Long getSijoitteluajoId() {
        return sijoitteluajoId;
    }

    public void setSijoitteluajoId(Long sijoitteluajoId) {
        this.sijoitteluajoId = sijoitteluajoId;
    }

    public String getTarjoajaOid() {
        return tarjoajaOid;
    }

    public void setTarjoajaOid(String tarjoajaOid) {
        this.tarjoajaOid = tarjoajaOid;
    }

    @Override
    public String toString() {
        return "Hakukohde{" + ", sijoitteluajoId=" + sijoitteluajoId + ", oid='" + oid + '\'' + ", tila=" + tila
                + ", valintatapajonot=" + valintatapajonot + ", hakijaryhmat=" + hakijaryhmat + '}';
    }
}
