package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

public class EPostiRequest {
    private String hakuOid;
    private String asiointikieli;
    private String kirjeenTyyppi;
    private String templateName;

    public String getHakuOid() {
        return hakuOid;
    }

    public void setHakuOid(String hakuOid) {
        this.hakuOid = hakuOid;
    }

    public String getAsiointikieli() {
        return asiointikieli;
    }

    public void setAsiointikieli(String asiointikieli) {
        this.asiointikieli = asiointikieli;
    }

    public String getKirjeenTyyppi() {
        return kirjeenTyyppi;
    }

    public void setKirjeenTyyppi(String kirjeenTyyppi) {
        this.kirjeenTyyppi = kirjeenTyyppi;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
}
