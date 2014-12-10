package fi.vm.sade.valinta.kooste.external.resource.authentication.dto;

import java.util.Date;

import fi.vm.sade.authentication.model.HenkiloTyyppi;
//import fi.vm.sade.authentication.model.Kayttajatiedot;
import fi.vm.sade.authentication.model.Kielisyys;

public class HenkiloCreateDTO {

    private String etunimet;
    private String kutsumanimi;
    private String sukunimi;
    private String hetu;
    private Date syntymaaika;
    private String sukupuoli;
    private Kielisyys asiointiKieli;
    //private Kayttajatiedot kayttajatiedot;
    private HenkiloTyyppi henkiloTyyppi;
    private String kasittelijaOid;
    //List<OrganisaatioHenkiloDTO> organisaatioHenkilo;

    public String getEtunimet() {
        return etunimet;
    }

    public void setEtunimet(String etunimet) {
        this.etunimet = etunimet;
    }

    public String getKutsumanimi() {
        return kutsumanimi;
    }

    public void setKutsumanimi(String kutsumanimi) {
        this.kutsumanimi = kutsumanimi;
    }

    public String getSukunimi() {
        return sukunimi;
    }

    public void setSukunimi(String sukunimi) {
        this.sukunimi = sukunimi;
    }

    public String getHetu() {
        return hetu;
    }

    public void setHetu(String hetu) {
        this.hetu = hetu;
    }

    public Date getSyntymaaika() {
        return syntymaaika;
    }

    public void setSyntymaaika(Date syntymaaika) {
        this.syntymaaika = syntymaaika;
    }

    public Kielisyys getAsiointiKieli() {
        return asiointiKieli;
    }

    public void setAsiointiKieli(Kielisyys asiointiKieli) {
        this.asiointiKieli = asiointiKieli;
    }

    /*public Kayttajatiedot getKayttajatiedot() {
        return kayttajatiedot;
    }

    public void setKayttajatiedot(Kayttajatiedot kayttajatiedot) {
        this.kayttajatiedot = kayttajatiedot;
    }*/

    public HenkiloTyyppi getHenkiloTyyppi() {
        return henkiloTyyppi;
    }

    public void setHenkiloTyyppi(HenkiloTyyppi henkiloTyyppi) {
        this.henkiloTyyppi = henkiloTyyppi;
    }

    /*public List<OrganisaatioHenkiloDTO> getOrganisaatioHenkilo() {
        return organisaatioHenkilo;
    }

    public void addOrganisaatioHenkilo(OrganisaatioHenkiloDTO organisaatioHenkilo) {
        if (this.organisaatioHenkilo == null) {
            this.organisaatioHenkilo = new ArrayList<OrganisaatioHenkiloDTO>();
        }
        this.organisaatioHenkilo.add(organisaatioHenkilo);
    }
    
    public void removeOrganisaatioHenkilo(OrganisaatioHenkiloDTO organisaatioHenkilo) {
        this.organisaatioHenkilo.remove(organisaatioHenkilo);
    }*/

    public String getSukupuoli() {
        return sukupuoli;
    }

    public void setSukupuoli(String sukupuoli) {
        this.sukupuoli = sukupuoli;
    }

    public String getKasittelijaOid() {
        return kasittelijaOid;
    }

    public void setKasittelijaOid(String kasittelijaOid) {
        this.kasittelijaOid = kasittelijaOid;
    }
}