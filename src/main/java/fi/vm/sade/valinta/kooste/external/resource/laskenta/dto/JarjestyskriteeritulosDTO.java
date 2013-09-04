package fi.vm.sade.valinta.kooste.external.resource.laskenta.dto;

import java.math.BigDecimal;

//@Converters(BigDecimalConverter.class)
public class JarjestyskriteeritulosDTO {

    private BigDecimal arvo;
    private JarjestyskriteerituloksenTila tila;
    private String kuvaus;

    public BigDecimal getArvo() {
        return arvo;
    }

    public void setArvo(BigDecimal arvo) {
        this.arvo = arvo;
    }

    public JarjestyskriteerituloksenTila getTila() {
        return tila;
    }

    public String getKuvaus() {
        return kuvaus;
    }

}