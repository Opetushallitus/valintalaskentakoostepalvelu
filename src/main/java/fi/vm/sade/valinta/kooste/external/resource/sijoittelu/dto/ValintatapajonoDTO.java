package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto;

import java.io.Serializable;
import java.util.ArrayList;

public class ValintatapajonoDTO implements Serializable {

    private Tasasijasaanto tasasijasaanto;
    private ValintatapajonoTila tila;
    private String oid;
    private Integer prioriteetti;
    private Integer aloituspaikat;
    private Boolean eiVarasijatayttoa;
    private ArrayList<HakemusDTO> hakemukset = new ArrayList<HakemusDTO>();

    public Tasasijasaanto getTasasijasaanto() {
        return tasasijasaanto;
    }

    public void setTasasijasaanto(Tasasijasaanto tasasijasaanto) {
        this.tasasijasaanto = tasasijasaanto;
    }

    public ValintatapajonoTila getTila() {
        return tila;
    }

    public void setTila(ValintatapajonoTila tila) {
        this.tila = tila;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public Integer getPrioriteetti() {
        return prioriteetti;
    }

    public void setPrioriteetti(Integer prioriteetti) {
        this.prioriteetti = prioriteetti;
    }

    public Integer getAloituspaikat() {
        return aloituspaikat;
    }

    public void setAloituspaikat(Integer aloituspaikat) {
        this.aloituspaikat = aloituspaikat;
    }

    public Boolean getEiVarasijatayttoa() {
        return eiVarasijatayttoa;
    }

    public void setEiVarasijatayttoa(Boolean eiVarasijatayttoa) {
        this.eiVarasijatayttoa = eiVarasijatayttoa;
    }

    public ArrayList<HakemusDTO> getHakemukset() {
        return hakemukset;
    }

    public void setHakemukset(ArrayList<HakemusDTO> hakemukset) {
        this.hakemukset = hakemukset;
    }
}
