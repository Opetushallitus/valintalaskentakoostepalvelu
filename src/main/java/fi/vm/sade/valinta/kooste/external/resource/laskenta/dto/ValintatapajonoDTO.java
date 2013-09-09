package fi.vm.sade.valinta.kooste.external.resource.laskenta.dto;

import java.util.ArrayList;
import java.util.List;

public class ValintatapajonoDTO {

    private String valintatapajonooid;
    private String nimi;
    private int prioriteetti;
    private int aloituspaikat;
    private boolean siirretaanSijoitteluun;
    private Tasasijasaanto tasasijasaanto;
    private Boolean eiVarasijatayttoa;
    private List<JonosijaDTO> jonosijat = new ArrayList<JonosijaDTO>();

    public String getNimi() {
        return nimi;
    }

    public String getValintatapajonooid() {
        return valintatapajonooid;
    }

    public void setValintatapajonooid(String valintatapajonooid) {
        this.valintatapajonooid = valintatapajonooid;
    }

    public void setNimi(String nimi) {
        this.nimi = nimi;
    }

    public boolean isSiirretaanSijoitteluun() {
        return siirretaanSijoitteluun;
    }

    public void setSiirretaanSijoitteluun(boolean siirretaanSijoitteluun) {
        this.siirretaanSijoitteluun = siirretaanSijoitteluun;
    }

    public int getPrioriteetti() {
        return prioriteetti;
    }

    public int getAloituspaikat() {
        return aloituspaikat;
    }

    public void setAloituspaikat(int aloituspaikat) {
        this.aloituspaikat = aloituspaikat;
    }

    public void setPrioriteetti(int prioriteetti) {
        this.prioriteetti = prioriteetti;
    }

    public String getOid() {
        return valintatapajonooid;
    }

    public void setOid(String oid) {
        this.valintatapajonooid = oid;
    }

    @Override
    public int hashCode() {
        return valintatapajonooid.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof ValintatapajonoDTO) {
            ValintatapajonoDTO vtj = (ValintatapajonoDTO) obj;
            return this == vtj;
        }
        return false;
    }

    public Tasasijasaanto getTasasijasaanto() {
        return tasasijasaanto;
    }

    public void setTasasijasaanto(Tasasijasaanto tasasijasaanto) {
        this.tasasijasaanto = tasasijasaanto;
    }

    public List<JonosijaDTO> getJonosijat() {
        return jonosijat;
    }

    public void setJonosijat(List<JonosijaDTO> jonosijat) {
        this.jonosijat = jonosijat;
    }

    public Boolean getEiVarasijatayttoa() {
        return eiVarasijatayttoa;
    }

    public void setEiVarasijatayttoa(Boolean eiVarasijatayttoa) {
        this.eiVarasijatayttoa = eiVarasijatayttoa;
    }
}