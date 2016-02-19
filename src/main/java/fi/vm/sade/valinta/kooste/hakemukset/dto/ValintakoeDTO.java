package fi.vm.sade.valinta.kooste.hakemukset.dto;

public class ValintakoeDTO {

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
