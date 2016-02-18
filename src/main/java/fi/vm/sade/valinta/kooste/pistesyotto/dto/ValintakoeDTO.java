package fi.vm.sade.valinta.kooste.pistesyotto.dto;

import java.io.Serializable;

public class ValintakoeDTO implements Serializable {

    private static final long serialVersionUID = -7076537760941865340L;

    private String tunniste;
    private Osallistuminen osallistuminen;
    private String pisteet;

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
