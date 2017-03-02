package fi.vm.sade.valinta.kooste.pistesyotto.dto;

public class ValintakoeDTO {

    private String tunniste;
    private Osallistuminen osallistuminen;
    private String pisteet;

    public ValintakoeDTO() {
    }

    public ValintakoeDTO(String tunniste, Osallistuminen osallistuminen, String pisteet) {
        this.tunniste = tunniste;
        this.osallistuminen = osallistuminen;
        this.pisteet = pisteet;
    }

    public String getTunniste() {
        return tunniste;
    }

    public void setTunniste(String tunniste) {
        this.tunniste = tunniste;
    }

    public Osallistuminen getOsallistuminen() {
        return osallistuminen;
    }

    public void setOsallistuminen(Osallistuminen osallistuminen) {
        this.osallistuminen = osallistuminen;
    }

    public String getPisteet() {
        return pisteet;
    }

    public void setPisteet(String pisteet) {
        this.pisteet = pisteet;
    }

    @Override
    public String toString() {
        return "ValintakoeDTO{" +
                "tunniste='" + tunniste + '\'' +
                ", osallistuminen=" + osallistuminen +
                ", pisteet='" + pisteet + '\'' +
                '}';
    }

    public enum Osallistuminen {
        OSALLISTUI, EI_OSALLISTUNUT
    }
}
