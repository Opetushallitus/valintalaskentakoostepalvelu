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
    private Boolean ohitaVastaanotto = null;
    private Boolean ohitaIlmoittautuminen = null;
    private IlmoittautumisTila ilmoittautumistila;
    private HakemuksenTila valinnantila;
    private ValintatuloksenTila vastaanottotila;
    private String ehdollisenHyvaksymisenEhtoKoodi;
    private String ehdollisenHyvaksymisenEhtoFI;
    private String ehdollisenHyvaksymisenEhtoSV;
    private String ehdollisenHyvaksymisenEhtoEN;

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

    public String getEhdollisenHyvaksymisenEhtoKoodi() { return ehdollisenHyvaksymisenEhtoKoodi; }

    public void setEhdollisenHyvaksymisenEhtoKoodi(String ehdollisenHyvaksymisenEhtoKoodi) {
        this.ehdollisenHyvaksymisenEhtoKoodi = ehdollisenHyvaksymisenEhtoKoodi;
    }

    public String getEhdollisenHyvaksymisenEhtoFI() { return ehdollisenHyvaksymisenEhtoFI;}

    public void setEhdollisenHyvaksymisenEhtoFI(String ehdollisenHyvaksymisenEhtoFI) {
        this.ehdollisenHyvaksymisenEhtoFI = ehdollisenHyvaksymisenEhtoFI;
    }

    public String getEhdollisenHyvaksymisenEhtoSV() { return ehdollisenHyvaksymisenEhtoSV;}

    public void setEhdollisenHyvaksymisenEhtoSV(String ehdollisenHyvaksymisenEhtoSV) {
        this.ehdollisenHyvaksymisenEhtoSV = ehdollisenHyvaksymisenEhtoSV;
    }

    public String getEhdollisenHyvaksymisenEhtoEN() { return ehdollisenHyvaksymisenEhtoEN;}

    public void setEhdollisenHyvaksymisenEhtoEN(String ehdollisenHyvaksymisenEhtoEN) {
        this.ehdollisenHyvaksymisenEhtoEN = ehdollisenHyvaksymisenEhtoEN;
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

    public Boolean getOhitaVastaanotto() {
        return ohitaVastaanotto;
    }

    public void setOhitaVastaanotto(Boolean ohitaVastaanotto) {
        this.ohitaVastaanotto = ohitaVastaanotto;
    }

    public Boolean getOhitaIlmoittautuminen() {
        return ohitaIlmoittautuminen;
    }

    public void setOhitaIlmoittautuminen(Boolean ohitaIlmoittautuminen) {
        this.ohitaIlmoittautuminen = ohitaIlmoittautuminen;
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
        return of(hakija, null);
    }

    public static Valinnantulos of(ErillishaunHakijaDTO hakija, Boolean ohitaVastaanotto) {
        Valinnantulos valinnantulos = new Valinnantulos();
        valinnantulos.setHakemusOid(hakija.hakemusOid);
        valinnantulos.setHenkiloOid(hakija.hakijaOid);
        valinnantulos.setHakukohdeOid(hakija.hakukohdeOid);
        valinnantulos.setValintatapajonoOid(hakija.valintatapajonoOid);

        valinnantulos.setEhdollisestiHyvaksyttavissa(hakija.ehdollisestiHyvaksyttavissa);
        valinnantulos.setEhdollisenHyvaksymisenEhtoKoodi(hakija.ehdollisenHyvaksymisenEhtoKoodi);
        valinnantulos.setEhdollisenHyvaksymisenEhtoFI(hakija.ehdollisenHyvaksymisenEhtoFI);
        valinnantulos.setEhdollisenHyvaksymisenEhtoSV(hakija.ehdollisenHyvaksymisenEhtoSV);
        valinnantulos.setEhdollisenHyvaksymisenEhtoEN(hakija.ehdollisenHyvaksymisenEhtoEN);
        valinnantulos.setJulkaistavissa(hakija.julkaistavissa);

        if(hakija.poistetaankoTulokset) {
            valinnantulos.setPoistettava(true);
        }

        if(null != ohitaVastaanotto && ohitaVastaanotto) {
            valinnantulos.setOhitaVastaanotto(true);
        }

        if(null == hakija.ilmoittautumisTila) {
            valinnantulos.setOhitaIlmoittautuminen(true);
        }

        valinnantulos.setValinnantila(hakija.hakemuksenTila);
        valinnantulos.setVastaanottotila(Optional.ofNullable(hakija.valintatuloksenTila).orElse(ValintatuloksenTila.KESKEN));
        valinnantulos.setIlmoittautumistila(Optional.ofNullable(hakija.ilmoittautumisTila).orElse(IlmoittautumisTila.EI_TEHTY));

        //TODO valinnantulos.setHyvaksyPeruuntunut(???);

        valinnantulos.setHyvaksyttyVarasijalta(hakija.getHakemuksenTila() == HakemuksenTila.VARASIJALTA_HYVAKSYTTY);

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
                ", ehdollisenHyvaksymisenEhtoKoodi=" + ehdollisenHyvaksymisenEhtoKoodi +
                ", ehdollisenHyvaksymisenEhtoFI=" + ehdollisenHyvaksymisenEhtoFI +
                ", ehdollisenHyvaksymisenEhtoSV=" + ehdollisenHyvaksymisenEhtoSV +
                ", ehdollisenHyvaksymisenEhtoEN=" + ehdollisenHyvaksymisenEhtoEN +
                ", julkaistavissa=" + julkaistavissa +
                ", hyvaksyttyVarasijalta=" + hyvaksyttyVarasijalta +
                ", hyvaksyPeruuntunut=" + hyvaksyPeruuntunut +
                ", poistettava=" + poistettava +
                ", ohitaVastaanotto=" + ohitaVastaanotto +
                ", ohitaIlmoittautuminen=" + ohitaIlmoittautuminen +
                ", ilmoittautumistila=" + ilmoittautumistila +
                ", valinnantila=" + valinnantila +
                ", vastaanottotila=" + vastaanottotila +
                '}';
    }
}