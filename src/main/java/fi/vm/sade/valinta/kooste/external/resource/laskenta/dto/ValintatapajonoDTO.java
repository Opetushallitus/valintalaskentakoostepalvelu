package fi.vm.sade.valinta.kooste.external.resource.laskenta.dto;

import java.util.ArrayList;
import java.util.List;

public class ValintatapajonoDTO implements Comparable<ValintatapajonoDTO> {

    private String valintatapajonooid;
    private Long versio;
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

    public Long getVersio() {
        return versio;
    }

    public void setVersio(Long versio) {
        this.versio = versio;
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

    public int compareTo(ValintatapajonoDTO o) {
        if (equals(o)) {
            return 0;
        }
        return versio.compareTo(o.versio);
    }

    public boolean equals(Object obj) {
        if (obj instanceof ValintatapajonoDTO) {
            ValintatapajonoDTO vtj = (ValintatapajonoDTO) obj;
            return this == vtj;
        }
        return false;
    }

    public int hashCode() {
        return versio.intValue();
    }

    public Tasasijasaanto getTasasijasaanto() {
        return tasasijasaanto;
    }

    public List<JonosijaDTO> getJonosijat() {
        return jonosijat;
    }

    public Boolean getEiVarasijatayttoa() {
        return eiVarasijatayttoa;
    }

}
