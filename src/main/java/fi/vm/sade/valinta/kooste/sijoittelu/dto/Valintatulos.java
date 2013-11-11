package fi.vm.sade.valinta.kooste.sijoittelu.dto;

import fi.vm.sade.sijoittelu.tulos.dto.ValintatuloksenTila;

public class Valintatulos {

    private String valintatapajonoOid;
    private String hakemusOid;
    private String hakukohdeOid;
    private String hakijaOid;
    private String hakuOid;
    private int hakutoive;
    private ValintatuloksenTila tila;

    public int getHakutoive() {
        return hakutoive;
    }

    public void setHakutoive(int hakutoive) {
        this.hakutoive = hakutoive;
    }

    public String getHakukohdeOid() {
        return hakukohdeOid;
    }

    public void setHakukohdeOid(String hakukohdeOid) {
        this.hakukohdeOid = hakukohdeOid;
    }

    public String getHakemusOid() {
        return hakemusOid;
    }

    public void setHakemusOid(String hakemusOid) {
        this.hakemusOid = hakemusOid;
    }

    public String getHakijaOid() {
        return hakijaOid;
    }

    public void setHakijaOid(String hakijaOid) {
        this.hakijaOid = hakijaOid;
    }

    public ValintatuloksenTila getTila() {
        return tila;
    }

    public void setTila(ValintatuloksenTila tila) {
        this.tila = tila;
    }

    public String getValintatapajonoOid() {
        return valintatapajonoOid;
    }

    public void setValintatapajonoOid(String valintatapajonoOid) {
        this.valintatapajonoOid = valintatapajonoOid;
    }

    public String getHakuOid() {
        return hakuOid;
    }

    public void setHakuOid(String hakuOid) {
        this.hakuOid = hakuOid;
    }

}
