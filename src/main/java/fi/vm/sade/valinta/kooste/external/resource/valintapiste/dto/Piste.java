package fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto;

public class Piste {
    private String tunniste;
    private String arvo;
    private Osallistuminen osallistuminen;
    private String tallettaja;

    public Osallistuminen getOsallistuminen() {
        return osallistuminen;
    }

    public String getArvo() {
        return arvo;
    }

    public String getTallettaja() {
        return tallettaja;
    }

    public String getTunniste() {
        return tunniste;
    }
}
