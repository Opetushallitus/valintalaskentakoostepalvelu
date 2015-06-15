package fi.vm.sade.valinta.kooste.valintatapajono.dto;

public class Kuvaus {
    private final String kieli;
    private final String teksti;

    public Kuvaus() {
        this.kieli = null;
        this.teksti = null;
    }

    public Kuvaus(String kieli, String teksti) {
        this.kieli = kieli;
        this.teksti = teksti;
    }

    public String getKieli() {
        return kieli;
    }

    public String getTeksti() {
        return teksti;
    }
}
