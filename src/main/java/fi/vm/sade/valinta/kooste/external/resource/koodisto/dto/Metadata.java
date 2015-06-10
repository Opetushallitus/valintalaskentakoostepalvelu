package fi.vm.sade.valinta.kooste.external.resource.koodisto.dto;

public class Metadata {
    private String nimi;
    private String kieli;

    public void setKieli(String kieli) {
        this.kieli = kieli;
    }

    public void setNimi(String nimi) {
        this.nimi = nimi;
    }

    public String getKieli() {
        return kieli;
    }

    public String getNimi() {
        return nimi;
    }
}
