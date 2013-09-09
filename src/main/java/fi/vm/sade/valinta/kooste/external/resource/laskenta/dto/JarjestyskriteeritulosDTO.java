package fi.vm.sade.valinta.kooste.external.resource.laskenta.dto;

import java.math.BigDecimal;

//@Converters(BigDecimalConverter.class)
public class JarjestyskriteeritulosDTO implements Comparable<JarjestyskriteeritulosDTO> {

    private BigDecimal arvo;
    private JarjestyskriteerituloksenTila tila;
    private String kuvaus;
    private int prioriteetti;

    @Override
    public int compareTo(JarjestyskriteeritulosDTO o) {
        return new Integer(prioriteetti).compareTo(o.getPrioriteetti());
    }

    public BigDecimal getArvo() {
        return arvo;
    }

    public void setArvo(BigDecimal arvo) {
        this.arvo = arvo;
    }

    public JarjestyskriteerituloksenTila getTila() {
        return tila;
    }

    public void setTila(JarjestyskriteerituloksenTila tila) {
        this.tila = tila;
    }

    public String getKuvaus() {
        return kuvaus;
    }

    public void setKuvaus(String kuvaus) {
        this.kuvaus = kuvaus;
    }

    public int getPrioriteetti() {
        return prioriteetti;
    }

    public void setPrioriteetti(int prioriteetti) {
        this.prioriteetti = prioriteetti;
    }

}