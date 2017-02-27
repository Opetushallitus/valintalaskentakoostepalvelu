package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto;

import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.sijoittelu.domain.HakemuksenTila;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;

import java.util.Optional;

public class Valinnantulos {
    private String hakukohdeOid;
    private String valintatapajonoOid;
    private String hakemusOid;
    private String henkiloOid;
    private Boolean ehdollisestiHyvaksyttavissa = null;
    private Boolean julkaistavissa = null;
    private Boolean hyvaksyttyVarasijalta = null;
    private Boolean hyvaksyPeruuntunut = null;
    private Boolean poistettava = null;
    private IlmoittautumisTila ilmoittautumistila;
    private HakemuksenTila valinnantila;
    private ValintatuloksenTila vastaanottotila;

    public String getHakukohdeOid() {
        return hakukohdeOid;
    }

    public void setHakukohdeOid(String hakukohdeOid) {
        this.hakukohdeOid = hakukohdeOid;
    }

    public String getValintatapajonoOid() {
        return valintatapajonoOid;
    }

    public void setValintatapajonoOid(String valintatapajonoOid) {
        this.valintatapajonoOid = valintatapajonoOid;
    }

    public String getHakemusOid() {
        return hakemusOid;
    }

    public void setHakemusOid(String hakemusOid) {
        this.hakemusOid = hakemusOid;
    }

    public String getHenkiloOid() {
        return henkiloOid;
    }

    public void setHenkiloOid(String henkiloOid) {
        this.henkiloOid = henkiloOid;
    }

    public Boolean getEhdollisestiHyvaksyttavissa() {
        return ehdollisestiHyvaksyttavissa;
    }

    public void setEhdollisestiHyvaksyttavissa(Boolean ehdollisestiHyvaksyttavissa) {
        this.ehdollisestiHyvaksyttavissa = ehdollisestiHyvaksyttavissa;
    }

    public Boolean getJulkaistavissa() {
        return julkaistavissa;
    }

    public void setJulkaistavissa(Boolean julkaistavissa) {
        this.julkaistavissa = julkaistavissa;
    }

    public Boolean getHyvaksyttyVarasijalta() {
        return hyvaksyttyVarasijalta;
    }

    public void setHyvaksyttyVarasijalta(Boolean hyvaksyttyVarasijalta) {
        this.hyvaksyttyVarasijalta = hyvaksyttyVarasijalta;
    }

    public Boolean getHyvaksyPeruuntunut() {
        return hyvaksyPeruuntunut;
    }

    public void setHyvaksyPeruuntunut(Boolean hyvaksyPeruuntunut) {
        this.hyvaksyPeruuntunut = hyvaksyPeruuntunut;
    }

    public Boolean getPoistettava() {
        return poistettava;
    }

    public void setPoistettava(Boolean poistettava) {
        this.poistettava = poistettava;
    }

    public IlmoittautumisTila getIlmoittautumistila() {
        return ilmoittautumistila;
    }

    public void setIlmoittautumistila(IlmoittautumisTila ilmoittautumistila) {
        this.ilmoittautumistila = ilmoittautumistila;
    }

    public HakemuksenTila getValinnantila() {
        return valinnantila;
    }

    public void setValinnantila(HakemuksenTila valinnantila) {
        this.valinnantila = valinnantila;
    }

    public ValintatuloksenTila getVastaanottotila() {
        return vastaanottotila;
    }

    public void setVastaanottotila(ValintatuloksenTila vastaanottotila) {
        this.vastaanottotila = vastaanottotila;
    }

    public static Valinnantulos of(ErillishaunHakijaDTO hakija) {
        Valinnantulos valinnantulos = new Valinnantulos();
        valinnantulos.setHakemusOid(hakija.hakemusOid);
        valinnantulos.setHenkiloOid(hakija.hakijaOid);
        valinnantulos.setHakukohdeOid(hakija.hakukohdeOid);
        valinnantulos.setValintatapajonoOid(hakija.valintatapajonoOid);

        valinnantulos.setEhdollisestiHyvaksyttavissa(hakija.ehdollisestiHyvaksyttavissa);
        valinnantulos.setJulkaistavissa(hakija.julkaistavissa);

        if(hakija.poistetaankoTulokset) {
            valinnantulos.setPoistettava(true);
        }

        valinnantulos.setValinnantila(hakija.hakemuksenTila);
        valinnantulos.setVastaanottotila(Optional.ofNullable(hakija.valintatuloksenTila).orElse(ValintatuloksenTila.KESKEN));
        valinnantulos.setIlmoittautumistila(Optional.ofNullable(hakija.ilmoittautumisTila).orElse(IlmoittautumisTila.EI_TEHTY));

        //TODO valinnantulos.setHyvaksyPeruuntunut(???);
        //TODO valinnantulos.setHyvaksyttyVarasijalta(???);

        return valinnantulos;
    }

    @Override
    public String toString() {
        return "Valinnantulos{" +
                "hakukohdeOid='" + hakukohdeOid + '\'' +
                ", valintatapajonoOid='" + valintatapajonoOid + '\'' +
                ", hakemusOid='" + hakemusOid + '\'' +
                ", henkiloOid='" + henkiloOid + '\'' +
                ", ehdollisestiHyvaksyttavissa=" + ehdollisestiHyvaksyttavissa +
                ", julkaistavissa=" + julkaistavissa +
                ", hyvaksyttyVarasijalta=" + hyvaksyttyVarasijalta +
                ", hyvaksyPeruuntunut=" + hyvaksyPeruuntunut +
                ", poistettava=" + poistettava +
                ", ilmoittautumistila=" + ilmoittautumistila +
                ", valinnantila=" + valinnantila +
                ", vastaanottotila=" + vastaanottotila +
                '}';
    }
}