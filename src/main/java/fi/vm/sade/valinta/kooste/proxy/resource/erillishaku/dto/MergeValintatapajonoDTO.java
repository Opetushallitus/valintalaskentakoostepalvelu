package fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.dto;

import fi.vm.sade.sijoittelu.tulos.dto.Tasasijasaanto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kjsaila on 26/01/15.
 */
public class MergeValintatapajonoDTO {

    private Tasasijasaanto tasasijasaanto = Tasasijasaanto.ARVONTA;

    private String oid;

    private String nimi;

    private int prioriteetti = 0;

    private int aloituspaikat = 0;

    private BigDecimal alinHyvaksyttyPistemaara;

    private boolean eiVarasijatayttoa = false;

    private boolean kaikkiEhdonTayttavatHyvaksytaan = false;

    private boolean poissaOlevaTaytto = false;

    private List<MergeHakemusDTO> hakemukset = new ArrayList<MergeHakemusDTO>();

    private int hakeneet = 0;

    private int hyvaksytty = 0;

    private int varalla = 0;

    private boolean kaytetaanValintalaskentaa = true;

    private boolean valmisSijoiteltavaksi = true;

    private boolean siirretaanSijoitteluun = true;

    private Long sijoitteluajoId;

    public Tasasijasaanto getTasasijasaanto() {
        return tasasijasaanto;
    }

    public void setTasasijasaanto(Tasasijasaanto tasasijasaanto) {
        this.tasasijasaanto = tasasijasaanto;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getNimi() {
        return nimi;
    }

    public void setNimi(String nimi) {
        this.nimi = nimi;
    }

    public int getPrioriteetti() {
        return prioriteetti;
    }

    public void setPrioriteetti(int prioriteetti) {
        this.prioriteetti = prioriteetti;
    }

    public int getAloituspaikat() {
        return aloituspaikat;
    }

    public void setAloituspaikat(int aloituspaikat) {
        this.aloituspaikat = aloituspaikat;
    }

    public BigDecimal getAlinHyvaksyttyPistemaara() {
        return alinHyvaksyttyPistemaara;
    }

    public void setAlinHyvaksyttyPistemaara(BigDecimal alinHyvaksyttyPistemaara) {
        this.alinHyvaksyttyPistemaara = alinHyvaksyttyPistemaara;
    }

    public boolean isEiVarasijatayttoa() {
        return eiVarasijatayttoa;
    }

    public void setEiVarasijatayttoa(boolean eiVarasijatayttoa) {
        this.eiVarasijatayttoa = eiVarasijatayttoa;
    }

    public boolean isKaikkiEhdonTayttavatHyvaksytaan() {
        return kaikkiEhdonTayttavatHyvaksytaan;
    }

    public void setKaikkiEhdonTayttavatHyvaksytaan(boolean kaikkiEhdonTayttavatHyvaksytaan) {
        this.kaikkiEhdonTayttavatHyvaksytaan = kaikkiEhdonTayttavatHyvaksytaan;
    }

    public boolean isPoissaOlevaTaytto() {
        return poissaOlevaTaytto;
    }

    public void setPoissaOlevaTaytto(boolean poissaOlevaTaytto) {
        this.poissaOlevaTaytto = poissaOlevaTaytto;
    }

    public int getHakeneet() {
        return hakeneet;
    }

    public void setHakeneet(int hakeneet) {
        this.hakeneet = hakeneet;
    }

    public int getHyvaksytty() {
        return hyvaksytty;
    }

    public void setHyvaksytty(int hyvaksytty) {
        this.hyvaksytty = hyvaksytty;
    }

    public int getVaralla() {
        return varalla;
    }

    public void setVaralla(int varalla) {
        this.varalla = varalla;
    }

    public List<MergeHakemusDTO> getHakemukset() {
        return hakemukset;
    }

    public void setHakemukset(List<MergeHakemusDTO> hakemukset) {
        this.hakemukset = hakemukset;
    }

    public boolean isKaytetaanValintalaskentaa() {
        return kaytetaanValintalaskentaa;
    }

    public void setKaytetaanValintalaskentaa(boolean kaytetaanValintalaskentaa) {
        this.kaytetaanValintalaskentaa = kaytetaanValintalaskentaa;
    }

    public boolean isValmisSijoiteltavaksi() {
        return valmisSijoiteltavaksi;
    }

    public void setValmisSijoiteltavaksi(boolean valmisSijoiteltavaksi) {
        this.valmisSijoiteltavaksi = valmisSijoiteltavaksi;
    }

    public boolean isSiirretaanSijoitteluun() {
        return siirretaanSijoitteluun;
    }

    public void setSiirretaanSijoitteluun(boolean siirretaanSijoitteluun) {
        this.siirretaanSijoitteluun = siirretaanSijoitteluun;
    }

    public Long getSijoitteluajoId() {
        return sijoitteluajoId;
    }

    public void setSijoitteluajoId(Long sijoitteluajoId) {
        this.sijoitteluajoId = sijoitteluajoId;
    }
}
