package fi.vm.sade.valinta.kooste.hakuimport.wrapper;

/**
 * User: wuoti
 * Date: 21.5.2013
 * Time: 13.11
 */
public class Hakukohde {
    public Hakukohde() {
    }

    public Hakukohde(String hakukohdeKoodiUri, String hakukohdeKoodiArvo, String hakukohdeOid, String nimiFi,
                     String nimiSv, String nimiEn) {
        this.hakukohdeKoodiUri = hakukohdeKoodiUri;
        this.hakukohdeKoodiArvo = hakukohdeKoodiArvo;
        this.hakukohdeOid = hakukohdeOid;
        this.nimiFi = nimiFi;
        this.nimiSv = nimiSv;
        this.nimiEn = nimiEn;
    }

    private String hakukohdeKoodiUri;
    private String hakukohdeKoodiArvo;
    private String hakukohdeOid;
    private String nimiFi;
    private String nimiSv;
    private String nimiEn;

    public String getHakukohdeKoodiUri() {
        return hakukohdeKoodiUri;
    }

    public void setHakukohdeKoodiUri(String hakukohdeKoodiUri) {
        this.hakukohdeKoodiUri = hakukohdeKoodiUri;
    }

    public String getHakukohdeKoodiArvo() {
        return hakukohdeKoodiArvo;
    }

    public void setHakukohdeKoodiArvo(String hakukohdeKoodiArvo) {
        this.hakukohdeKoodiArvo = hakukohdeKoodiArvo;
    }

    public String getHakukohdeOid() {
        return hakukohdeOid;
    }

    public void setHakukohdeOid(String hakukohdeOid) {
        this.hakukohdeOid = hakukohdeOid;
    }

    public String getNimiFi() {
        return nimiFi;
    }

    public void setNimiFi(String nimiFi) {
        this.nimiFi = nimiFi;
    }

    public String getNimiSv() {
        return nimiSv;
    }

    public void setNimiSv(String nimiSv) {
        this.nimiSv = nimiSv;
    }

    public String getNimiEn() {
        return nimiEn;
    }

    public void setNimiEn(String nimiEn) {
        this.nimiEn = nimiEn;
    }

}
