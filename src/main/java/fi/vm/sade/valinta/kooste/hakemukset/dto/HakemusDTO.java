package fi.vm.sade.valinta.kooste.hakemukset.dto;

import java.io.Serializable;
import java.util.List;

public class HakemusDTO implements Serializable {

    private static final long serialVersionUID = -7990602838416651035L;

    private String hakemusOid;
    private String henkiloOid;
    private String etunimet;
    private String sukunimi;
    private String kutsumanimi;
    private String sahkoposti;
    private String katuosoite;
    private String postinumero;
    private String postitoimipaikka;

    private List<HakukohdeDTO> hakukohteet;

    public String getHakemusOid() {
        return hakemusOid;
    }

    public void setHakemusOid(String hakemusOid) {
        this.hakemusOid = hakemusOid;
    }

    public String getHenkiloOid() {
        return henkiloOid;
    }

    public void setHenkiloOid(String henkiloOid) {
        this.henkiloOid = henkiloOid;
    }

    public String getEtunimet() {
        return etunimet;
    }

    public void setEtunimet(String etunimet) {
        this.etunimet = etunimet;
    }

    public String getSukunimi() {
        return sukunimi;
    }

    public void setSukunimi(String sukunimi) {
        this.sukunimi = sukunimi;
    }

    public String getKutsumanimi() {
        return kutsumanimi;
    }

    public void setKutsumanimi(String kutsumanimi) {
        this.kutsumanimi = kutsumanimi;
    }

    public String getSahkoposti() {
        return sahkoposti;
    }

    public void setSahkoposti(String sahkoposti) {
        this.sahkoposti = sahkoposti;
    }

    public String getKatuosoite() {
        return katuosoite;
    }

    public void setKatuosoite(String katuosoite) {
        this.katuosoite = katuosoite;
    }

    public String getPostinumero() {
        return postinumero;
    }

    public void setPostinumero(String postinumero) {
        this.postinumero = postinumero;
    }

    public String getPostitoimipaikka() {
        return postitoimipaikka;
    }

    public void setPostitoimipaikka(String postitoimipaikka) {
        this.postitoimipaikka = postitoimipaikka;
    }

    public List<HakukohdeDTO> getHakukohteet() {
        return hakukohteet;
    }

    public void setHakukohteet(List<HakukohdeDTO> hakukohteet) {
        this.hakukohteet = hakukohteet;
    }

    @Override
    public String toString() {
        return "HakemusDTO{" +
                "hakemusOid='" + hakemusOid + '\'' +
                ", henkiloOid='" + henkiloOid + '\'' +
                ", etunimet='" + etunimet + '\'' +
                ", sukunimi='" + sukunimi + '\'' +
                ", kutsumanimi='" + kutsumanimi + '\'' +
                ", sahkoposti='" + sahkoposti + '\'' +
                ", katuosoite='" + katuosoite + '\'' +
                ", postinumero='" + postinumero + '\'' +
                ", postitoimipaikka='" + postitoimipaikka + '\'' +
                ", hakukohteet=" + hakukohteet +
                '}';
    }
}
