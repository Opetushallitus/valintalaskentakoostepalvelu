package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;

public class KoekutsuDTO {
    private final String letterBodyText;
    private final String tarjoajaOid;
    private final String tag;
    private final String hakukohdeOid;
    private final HakuV1RDTO haku;
    private final String templateName;

    public KoekutsuDTO() {
        this.letterBodyText = null;
        this.tarjoajaOid = null;
        this.tag = null;
        this.hakukohdeOid = null;
        this.haku = null;
        this.templateName = null;
    }

    public KoekutsuDTO(String letterBodyText, String tarjoajaOid, String tag,
                       String hakukohdeOid, HakuV1RDTO haku, String templateName) {
        this.letterBodyText = letterBodyText;
        this.tarjoajaOid = tarjoajaOid;
        this.tag = tag;
        this.hakukohdeOid = hakukohdeOid;
        this.haku = haku;
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getHakukohdeOid() {
        return hakukohdeOid;
    }

    public HakuV1RDTO getHaku() {
        return haku;
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
