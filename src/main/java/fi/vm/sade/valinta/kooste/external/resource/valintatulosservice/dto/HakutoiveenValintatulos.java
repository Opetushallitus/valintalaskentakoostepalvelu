package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto;

import java.util.Date;
import java.util.Map;

public class HakutoiveenValintatulos {
    private String hakukohdeOid;
    private String tarjoajaOid;
    private String hakukohdeNimi;
    private String tarjoajaNimi;
    private String valintatila;
    private String vastaanottotila;
    private HakutoiveenIlmoittautumistila ilmoittautumistila;
    private String vastaanotettavuustila;
    private Date vastaanottoDeadline;
    private Integer jonosija;
    private Date varasijojaKaytetaanAlkaen;
    private Date varasijojaTaytetaanAsti;
    private Integer varasijanumero;
    private Map<String, String> tilanKuvaukset;

    public HakutoiveenValintatulos(String hakukohdeOid, String tarjoajaOid, String valintatila, String vastaanottotila, HakutoiveenIlmoittautumistila ilmoittautumistila, String vastaanotettavuustila, Date vastaanottoDeadline,
                                   Integer jonosija, Date varasijojaKaytetaanAlkaen, Date varasijojaTaytetaanAsti, Integer varasijanumero, Map<String, String> tilanKuvaukset) {
        this.hakukohdeOid = hakukohdeOid;
        this.tarjoajaOid = tarjoajaOid;
        this.valintatila = valintatila;
        this.vastaanottotila = vastaanottotila;
        this.ilmoittautumistila = ilmoittautumistila;
        this.vastaanotettavuustila = vastaanotettavuustila;
        this.vastaanottoDeadline = vastaanottoDeadline;
        this.jonosija = jonosija;
        this.varasijojaKaytetaanAlkaen = varasijojaKaytetaanAlkaen;
        this.varasijojaTaytetaanAsti = varasijojaTaytetaanAsti;
        this.varasijanumero = varasijanumero;
        this.tilanKuvaukset = tilanKuvaukset;
    }

    public String getHakukohdeOid() {
        return hakukohdeOid;
    }

    public String getTarjoajaOid() {
        return tarjoajaOid;
    }

    public String getValintatila() {
        return valintatila;
    }

    public String getVastaanottotila() {
        return vastaanottotila;
    }

    public HakutoiveenIlmoittautumistila getIlmoittautumistila() {
        return ilmoittautumistila;
    }

    public String getVastaanotettavuustila() {
        return vastaanotettavuustila;
    }

    public Date getVastaanottoDeadline() {
        return vastaanottoDeadline;
    }

    public Integer getJonosija() {
        return jonosija;
    }

    public Date getVarasijojaKaytetaanAlkaen() {
        return varasijojaKaytetaanAlkaen;
    }

    public Date getVarasijojaTaytetaanAsti() {
        return varasijojaTaytetaanAsti;
    }

    public Integer getVarasijanumero() {
        return varasijanumero;
    }

    public Map<String, String> getTilanKuvaukset() {
        return tilanKuvaukset;
    }

    public String getHakukohdeNimi() {
        return hakukohdeNimi;
    }

    public String getTarjoajaNimi() {
        return tarjoajaNimi;
    }
}
