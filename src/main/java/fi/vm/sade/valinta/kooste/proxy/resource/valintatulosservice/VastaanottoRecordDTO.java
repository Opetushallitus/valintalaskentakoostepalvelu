package fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.domain.VastaanotettavuusDTO.VastaanottoAction;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class VastaanottoRecordDTO {
    private String henkiloOid;
    private String hakemusOid;
    private String hakuOid;
    private String hakukohdeOid;
    private String ilmoittaja;
    private VastaanottoAction action;

    public String getHenkiloOid() {
        return henkiloOid;
    }

    public void setHenkiloOid(String henkiloOid) {
        this.henkiloOid = henkiloOid;
    }

    public String getHakemusOid() {
        return hakemusOid;
    }

    public void setHakemusOid(String hakemusOid) {
        this.hakemusOid = hakemusOid;
    }

    public String getHakuOid() {
        return hakuOid;
    }

    public void setHakuOid(String hakuOid) {
        this.hakuOid = hakuOid;
    }

    public String getHakukohdeOid() {
        return hakukohdeOid;
    }

    public void setHakukohdeOid(String hakukohdeOid) {
        this.hakukohdeOid = hakukohdeOid;
    }

    public String getIlmoittaja() {
        return ilmoittaja;
    }

    public void setIlmoittaja(String ilmoittaja) {
        this.ilmoittaja = ilmoittaja;
    }

    public VastaanottoAction getAction() {
        return action;
    }

    public void setAction(VastaanottoAction action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static VastaanottoRecordDTO of(Valintatulos valintatulos, String muokkaaja) {
        VastaanottoRecordDTO v = new VastaanottoRecordDTO();
        v.setHenkiloOid(valintatulos.getHakijaOid());
        v.setHakemusOid(valintatulos.getHakemusOid());
        v.setHakuOid(valintatulos.getHakuOid());
        v.setHakukohdeOid(valintatulos.getHakukohdeOid());
        v.setIlmoittaja(muokkaaja);
        v.setAction(VastaanottoAction.of(valintatulos.getTila()));
        return v;
    }
}
