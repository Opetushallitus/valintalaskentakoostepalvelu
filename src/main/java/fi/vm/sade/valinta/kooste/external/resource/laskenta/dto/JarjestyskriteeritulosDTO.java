package fi.vm.sade.valinta.kooste.external.resource.laskenta.dto;

import java.math.BigDecimal;

//@Converters(BigDecimalConverter.class)
public class JarjestyskriteeritulosDTO {

    private BigDecimal arvo;
    private JarjestyskriteerituloksenTila tila;
    private String kuvaus;
    private int prioriteetti;

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