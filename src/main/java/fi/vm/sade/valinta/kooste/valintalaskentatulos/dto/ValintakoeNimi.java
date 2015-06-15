package fi.vm.sade.valinta.kooste.valintalaskentatulos.dto;

public class ValintakoeNimi {

    private final String nimi;
    private final String selvitettyTunniste;

    public ValintakoeNimi(String nimi, String selvitettyTunniste) {
        this.nimi = nimi;
        this.selvitettyTunniste = selvitettyTunniste;
    }

    public String getNimi() {
        return nimi;
    }

    public String getSelvitettyTunniste() {
        return selvitettyTunniste;
    }
}
