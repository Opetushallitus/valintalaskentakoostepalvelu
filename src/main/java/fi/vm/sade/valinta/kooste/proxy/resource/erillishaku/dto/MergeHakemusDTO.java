package fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.dto;

import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.valintalaskenta.domain.dto.JarjestyskriteeritulosDTO;

import java.math.BigDecimal;
import java.util.SortedSet;
import java.util.TreeSet;

public class MergeHakemusDTO {
    // Perustiedot
    private String etunimi;
    private String sukunimi;
    private String sahkoposti;
    private String henkilotunnus;
    private String hakijaOid;
    private String hakemusOid;
    private String syntymaaika;

    // flägit
    private boolean loytyiSijoittelusta = false;
    private boolean loytyiHakemuksista = false;
    private boolean loytyiLaskennasta = false;

    // Sijoittelun tiedot
    private BigDecimal pisteet;
    private BigDecimal paasyJaSoveltuvuusKokeenTulos;
    private HakemuksenTila hakemuksentila;
    private ValintatuloksenTila valintatuloksentila;
    private IlmoittautumisTila ilmoittautumistila;
    private int prioriteetti;
    private int jonosija;
    private int tasasijaJonosija;
    private int varasijanNumero;
    private int todellinenJonosija;
    private boolean julkaistavissa = false;
    private boolean hyvaksyttyVarasijalta = false;

    // Laskennan tiedot
    private SortedSet<JarjestyskriteeritulosDTO> jarjestyskriteerit = new TreeSet<JarjestyskriteeritulosDTO>();
    private boolean harkinnanvarainen = false;
    private boolean muokattu = false;
    private boolean hylattyValisijoittelussa = false;

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

    public String getSyntymaaika() {
        return syntymaaika;
    }

    public void setSyntymaaika(String syntymaaika) {
        this.syntymaaika = syntymaaika;
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

    public boolean isLoytyiLaskennasta() {
        return loytyiLaskennasta;
    }

    public void setLoytyiLaskennasta(boolean loytyiLaskennasta) {
        this.loytyiLaskennasta = loytyiLaskennasta;
    }

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

    public int getTasasijaJonosija() {
        return tasasijaJonosija;
    }

    public void setTasasijaJonosija(int tasasijaJonosija) {
        this.tasasijaJonosija = tasasijaJonosija;
    }

    public int getVarasijanNumero() {
        return varasijanNumero;
    }

    public void setVarasijanNumero(int varasijanNumero) {
        this.varasijanNumero = varasijanNumero;
    }

    public int getTodellinenJonosija() {
        return todellinenJonosija;
    }

    public void setTodellinenJonosija(int todellinenJonosija) {
        this.todellinenJonosija = todellinenJonosija;
    }

    public BigDecimal getPisteet() {
        return pisteet;
    }

    public void setPisteet(BigDecimal pisteet) {
        this.pisteet = pisteet;
    }

    public BigDecimal getPaasyJaSoveltuvuusKokeenTulos() {
        return paasyJaSoveltuvuusKokeenTulos;
    }

    public void setPaasyJaSoveltuvuusKokeenTulos(BigDecimal paasyJaSoveltuvuusKokeenTulos) {
        this.paasyJaSoveltuvuusKokeenTulos = paasyJaSoveltuvuusKokeenTulos;
    }

    public SortedSet<JarjestyskriteeritulosDTO> getJarjestyskriteerit() {
        return jarjestyskriteerit;
    }

    public void setJarjestyskriteerit(SortedSet<JarjestyskriteeritulosDTO> jarjestyskriteerit) {
        this.jarjestyskriteerit = jarjestyskriteerit;
    }

    public boolean isHarkinnanvarainen() {
        return harkinnanvarainen;
    }

    public void setHarkinnanvarainen(boolean harkinnanvarainen) {
        this.harkinnanvarainen = harkinnanvarainen;
    }

    public boolean isMuokattu() {
        return muokattu;
    }

    public void setMuokattu(boolean muokattu) {
        this.muokattu = muokattu;
    }

    public boolean isHylattyValisijoittelussa() {
        return hylattyValisijoittelussa;
    }

    public void setHylattyValisijoittelussa(boolean hylattyValisijoittelussa) {
        this.hylattyValisijoittelussa = hylattyValisijoittelussa;
    }

    public boolean isJulkaistavissa() {
        return julkaistavissa;
    }

    public void setJulkaistavissa(boolean julkaistavissa) {
        this.julkaistavissa = julkaistavissa;
    }

    public boolean isHyvaksyttyVarasijalta() {
        return hyvaksyttyVarasijalta;
    }

    public void setHyvaksyttyVarasijalta(boolean hyvaksyttyVarasijalta) {
        this.hyvaksyttyVarasijalta = hyvaksyttyVarasijalta;
    }
}
