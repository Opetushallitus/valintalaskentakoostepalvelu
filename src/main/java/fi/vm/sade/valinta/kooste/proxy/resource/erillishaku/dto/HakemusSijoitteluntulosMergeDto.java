package fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.dto;

import fi.vm.sade.sijoittelu.domain.HakemuksenTila;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;

/**
 * Created by jussija on 26/01/15.
 */
public class HakemusSijoitteluntulosMergeDto {

    private String etunimi;
    private String sukunimi;
    private String sahkoposti;
    private String henkilotunnus;
    private String hakijaOid;
    private String hakemusOid;
    private HakemuksenTila hakemuksentila;
    private ValintatuloksenTila valintatuloksentila;
    private IlmoittautumisTila ilmoittautumistila;

    private boolean loytyiSijoittelusta;
    private boolean loytyiHakemuksista;

    public HakemuksenTila getHakemuksentila() {
        return hakemuksentila;
    }

    public IlmoittautumisTila getIlmoittautumistila() {
        return ilmoittautumistila;
    }

    public String getEtunimi() {
        return etunimi;
    }

    public String getHakemusOid() {
        return hakemusOid;
    }

    public String getHakijaOid() {
        return hakijaOid;
    }

    public String getHenkilotunnus() {
        return henkilotunnus;
    }

    public String getSahkoposti() {
        return sahkoposti;
    }

    public String getSukunimi() {
        return sukunimi;
    }

    public ValintatuloksenTila getValintatuloksentila() {
        return valintatuloksentila;
    }

    public void setEtunimi(String etunimi) {
        this.etunimi = etunimi;
    }

    public void setHakemuksentila(HakemuksenTila hakemuksentila) {
        this.hakemuksentila = hakemuksentila;
    }

    public void setHakemusOid(String hakemusOid) {
        this.hakemusOid = hakemusOid;
    }

    public void setHakijaOid(String hakijaOid) {
        this.hakijaOid = hakijaOid;
    }

    public void setHenkilotunnus(String henkilotunnus) {
        this.henkilotunnus = henkilotunnus;
    }

    public void setIlmoittautumistila(IlmoittautumisTila ilmoittautumistila) {
        this.ilmoittautumistila = ilmoittautumistila;
    }

    public void setLoytyiHakemuksista(boolean loytyiHakemuksista) {
        this.loytyiHakemuksista = loytyiHakemuksista;
    }

    public void setLoytyiSijoittelusta(boolean loytyiSijoittelusta) {
        this.loytyiSijoittelusta = loytyiSijoittelusta;
    }

    public void setSahkoposti(String sahkoposti) {
        this.sahkoposti = sahkoposti;
    }

    public void setSukunimi(String sukunimi) {
        this.sukunimi = sukunimi;
    }

    public void setValintatuloksentila(ValintatuloksenTila valintatuloksentila) {
        this.valintatuloksentila = valintatuloksentila;
    }

    public boolean isLoytyiHakemuksista() {
        return loytyiHakemuksista;
    }

    public boolean isLoytyiSijoittelusta() {
        return loytyiSijoittelusta;
    }
}
