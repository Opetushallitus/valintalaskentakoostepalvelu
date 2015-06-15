package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

public class KoekutsuDTO {
    private final String letterBodyText;
    private final String tarjoajaOid;
    private final String tag;
    private final String hakukohdeOid;
    private final String hakuOid;
    private final String templateName;

    public KoekutsuDTO() {
        this.letterBodyText = null;
        this.tarjoajaOid = null;
        this.tag = null;
        this.hakukohdeOid = null;
        this.hakuOid = null;
        this.templateName = null;
    }

    public KoekutsuDTO(String letterBodyText, String tarjoajaOid, String tag,
                       String hakukohdeOid, String hakuOid, String templateName) {
        this.letterBodyText = letterBodyText;
        this.tarjoajaOid = tarjoajaOid;
        this.tag = tag;
        this.hakukohdeOid = hakukohdeOid;
        this.hakuOid = hakuOid;
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getHakukohdeOid() {
        return hakukohdeOid;
    }

    public String getHakuOid() {
        return hakuOid;
    }

    public String getLetterBodyText() {
        return letterBodyText;
    }

    public String getTag() {
        return tag;
    }

    public String getTarjoajaOid() {
        return tarjoajaOid;
    }
}
