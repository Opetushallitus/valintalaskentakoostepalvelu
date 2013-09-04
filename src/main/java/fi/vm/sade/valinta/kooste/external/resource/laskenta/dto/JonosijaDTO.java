package fi.vm.sade.valinta.kooste.external.resource.laskenta.dto;

import java.util.TreeMap;

public class JonosijaDTO {

    private int jonosija;
    private String hakemusOid;
    private String hakijaOid;
    private TreeMap<Integer, JarjestyskriteeritulosDTO> jarjestyskriteerit = new TreeMap<Integer, JarjestyskriteeritulosDTO>();
    private int prioriteetti;
    private String sukunimi;
    private String etunimi;
    private boolean harkinnanvarainen = false;
    private JarjestyskriteerituloksenTila tuloksenTila;
    private boolean muokattu = false;

    public TreeMap<Integer, JarjestyskriteeritulosDTO> getJarjestyskriteerit() {
        return jarjestyskriteerit;
    }

    public String getHakijaOid() {
        return hakijaOid;
    }

    public String getHakemusOid() {
        return hakemusOid;
    }

    public int getJonosija() {
        return jonosija;
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

    public boolean isMuokattu() {
        return muokattu;
    }
}
