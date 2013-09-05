package fi.vm.sade.valinta.kooste.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Geneerinen vastaus viesti koostepalvelulta. Modaalisen dialogin
 *         esittämiseen erinäisistä onnistumis/virhe skenaarioista!
 */
public class Vastaus {

    private String viesti;
    private String latausUrl;

    private Vastaus() {
    }

    private Vastaus(String latausUrl) {
        this.latausUrl = latausUrl;
    }

    public String getLatausUrl() {
        return latausUrl;
    }

    public String getViesti() {
        return viesti;
    }

    public static Vastaus uudelleenOhjaus(String uudelleenOhjausUrl) {
        return new Vastaus(uudelleenOhjausUrl);
    }

    public static Vastaus virhe(String virheViesti) {
        Vastaus vastaus = new Vastaus();
        vastaus.viesti = virheViesti;
        return vastaus;
    }
}
