package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto;

import java.io.Serializable;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Koska sijoittelulla ei ole omaa API:a!
 */
public class Hakemus implements Serializable {

    private String hakijaOid;
    private String hakemusOid;
    private String etunimi;
    private String sukunimi;
    private Integer prioriteetti;
    private Integer jonosija;
    private Integer tasasijaJonosija;
    private HakemuksenTila tila;
    private boolean hyvaksyttyHarkinnanvaraisesti = false;

    public int getPrioriteetti() {
        return prioriteetti;
    }

    public void setPrioriteetti(int prioriteetti) {
        this.prioriteetti = prioriteetti;
    }

    public int getJonosija() {
        return jonosija;
    }

    public void setJonosija(int jonosija) {
        this.jonosija = jonosija;
    }

    public HakemuksenTila getTila() {
        return tila;
    }

    public void setTila(HakemuksenTila tila) {
        this.tila = tila;
    }

    public String getHakijaOid() {
        return hakijaOid;
    }

    public void setHakijaOid(String hakijaOid) {
        this.hakijaOid = hakijaOid;
    }

    public Integer getTasasijaJonosija() {
        return tasasijaJonosija;
    }

    public void setTasasijaJonosija(Integer tasasijaJonosija) {
        this.tasasijaJonosija = tasasijaJonosija;
    }

    public String getHakemusOid() {
        return hakemusOid;
    }

    public void setHakemusOid(String hakemusOid) {
        this.hakemusOid = hakemusOid;
    }

    @Override
    public String toString() {
        return "Hakemus{" + "hakijaOid='" + hakijaOid + '\'' + ", prioriteetti=" + prioriteetti + ", jonosija="
                + jonosija + ", tasasijaJonosija=" + tasasijaJonosija + ", tila=" + tila + '}';
    }

    public String getSukunimi() {
        return sukunimi;
    }

    public void setSukunimi(String sukunimi) {
        this.sukunimi = sukunimi;
    }

    public String getEtunimi() {
        return etunimi;
    }

    public void setEtunimi(String etunimi) {
        this.etunimi = etunimi;
    }

    public boolean isHyvaksyttyHarkinnanvaraisesti() {
        return hyvaksyttyHarkinnanvaraisesti;
    }

    public void setHyvaksyttyHarkinnanvaraisesti(boolean hyvaksyttyHarkinnanvaraisesti) {
        this.hyvaksyttyHarkinnanvaraisesti = hyvaksyttyHarkinnanvaraisesti;
    }
}