package fi.vm.sade.valinta.kooste.external.resource.laskenta.dto;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class JonosijaDTO {

    private int jonosija;
    private String hakemusOid;
    private String hakijaOid;
    private SortedSet<JarjestyskriteeritulosDTO> jarjestyskriteerit = new TreeSet<JarjestyskriteeritulosDTO>();
    private int prioriteetti;
    private String sukunimi;
    private String etunimi;
    private boolean harkinnanvarainen = false;
    private JarjestyskriteerituloksenTila tuloksenTila;
    private List<String> historiat;
    private boolean muokattu = false;

    public List<String> getHistoriat() {
        return historiat;
    }

    public void setHistoriat(List<String> historiat) {
        this.historiat = historiat;
    }

    public SortedSet<JarjestyskriteeritulosDTO> getJarjestyskriteerit() {
        return jarjestyskriteerit;
    }

    public void setJarjestyskriteerit(SortedSet<JarjestyskriteeritulosDTO> jarjestyskriteerit) {
        this.jarjestyskriteerit = jarjestyskriteerit;
    }

    public String getHakijaOid() {
        return hakijaOid;
    }

    public void setHakijaOid(String hakijaOid) {
        this.hakijaOid = hakijaOid;
    }

    public String getHakemusOid() {
        return hakemusOid;
    }

    public void setHakemusOid(String hakemusOid) {
        this.hakemusOid = hakemusOid;
    }

    public int getJonosija() {
        return jonosija;
    }

    public void setJonosija(int jonosija) {
        this.jonosija = jonosija;
    }

    public void setEtunimi(String etunimi) {
        this.etunimi = etunimi;
    }

    public void setSukunimi(String sukunimi) {
        this.sukunimi = sukunimi;
    }

    public void setPrioriteetti(int prioriteetti) {
        this.prioriteetti = prioriteetti;
    }

    public int getPrioriteetti() {
        return prioriteetti;
    }

    public String getSukunimi() {
        return sukunimi;
    }

    public String getEtunimi() {
        return etunimi;
    }

    public JarjestyskriteerituloksenTila getTuloksenTila() {
        return tuloksenTila;
    }

    public void setTuloksenTila(JarjestyskriteerituloksenTila tuloksenTila) {
        this.tuloksenTila = tuloksenTila;
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
}
