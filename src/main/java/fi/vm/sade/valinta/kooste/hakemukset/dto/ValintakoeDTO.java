package fi.vm.sade.valinta.kooste.hakemukset.dto;

import java.io.Serializable;

public class ValintakoeDTO implements Serializable {

    private static final long serialVersionUID = -445917363057078634L;

    private String tunniste;

    public String getTunniste() {
        return tunniste;
    }

    public void setTunniste(String tunniste) {
        this.tunniste = tunniste;
    }

    @Override
    public String toString() {
        return "ValintakoeDTO{" +
                "tunniste='" + tunniste + '\'' +
                '}';
    }
}
