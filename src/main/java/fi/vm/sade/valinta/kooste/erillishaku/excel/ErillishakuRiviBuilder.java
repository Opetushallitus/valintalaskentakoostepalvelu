package fi.vm.sade.valinta.kooste.erillishaku.excel;

import java.util.Date;

public class ErillishakuRiviBuilder {
    private String etunimi;
    private String sukunimi;
    private String hakemusOid;
    private String henkilotunnus;
    private String sahkoposti;
    private String syntymaAika;
    private Sukupuoli sukupuoli = Sukupuoli.EI_SUKUPUOLTA;
    private String aidinkieli;
    private String personOid;
    private String hakemuksenTila;
    private boolean ehdollisestiHyvaksyttavissa = false;
    private String ehdollisenHyvaksymisenEhtoKoodi = "";
    private String ehdollisenHyvaksymisenEhtoFI = "";
    private String ehdollisenHyvaksymisenEhtoSV = "";
    private String ehdollisenHyvaksymisenEhtoEN = "";
    private Date hyvaksymiskirjeLahetetty;
    private String vastaanottoTila = "";
    private String ilmoittautumisTila = "";
    private boolean julkaistaankoTiedot = false;
    private boolean poistetaankoRivi = false;
    private String maksuvelvollisuus;
    private String asiointikieli;
    private String puhelinnumero;
    private String osoite;
    private String postinumero;
    private String postitoimipaikka;
    private String asuinmaa;
    private String kansalaisuus;
    private String kotikunta;
    private Boolean toisenAsteenSuoritus;
    private String toisenAsteenSuoritusmaa;

    public ErillishakuRiviBuilder() {}

    public static ErillishakuRiviBuilder fromRivi(ErillishakuRivi rivi) {
        return new ErillishakuRiviBuilder()
                .hakemusOid(rivi.getHakemusOid())
                .sukunimi(rivi.getSukunimi())
                .etunimi(rivi.getEtunimi())
                .henkilotunnus(rivi.getHenkilotunnus())
                .sahkoposti(rivi.getSahkoposti())
                .syntymaAika(rivi.getSyntymaAika())
                .sukupuoli(rivi.getSukupuoli())
                .personOid(rivi.getPersonOid())
                .aidinkieli(rivi.getAidinkieli())
                .hakemuksenTila(rivi.getHakemuksenTila())
                .ehdollisestiHyvaksyttavissa(rivi.getEhdollisestiHyvaksyttavissa())
                .hyvaksymiskirjeLahetetty(rivi.getHyvaksymiskirjeLahetetty())
                .vastaanottoTila(rivi.getVastaanottoTila())
                .ilmoittautumisTila(rivi.getIlmoittautumisTila())
                .julkaistaankoTiedot(rivi.isJulkaistaankoTiedot())
                .poistetaankoRivi(rivi.isPoistetaankoRivi())
                .asiointikieli(rivi.getAsiointikieli())
                .puhelinnumero(rivi.getPuhelinnumero())
                .osoite(rivi.getOsoite())
                .postinumero(rivi.getPostinumero())
                .postitoimipaikka(rivi.getPostitoimipaikka())
                .asuinmaa(rivi.getAsuinmaa())
                .kansalaisuus(rivi.getKansalaisuus())
                .kotikunta(rivi.getKotikunta())
                .toisenAsteenSuoritus(rivi.getToisenAsteenSuoritus())
                .toisenAsteenSuoritusmaa(rivi.getToisenAsteenSuoritusmaa())
                .maksuvelvollisuus(rivi.getMaksuvelvollisuus());
    }

    public ErillishakuRiviBuilder etunimi(String etunimi) {
        this.etunimi = etunimi;
        return this;
    }

    public ErillishakuRiviBuilder sukunimi(String sukunimi) {
        this.sukunimi = sukunimi;
        return this;
    }

    public ErillishakuRiviBuilder hakemusOid(String hakemusOid) {
        this.hakemusOid = hakemusOid;
        return this;
    }

    public ErillishakuRiviBuilder henkilotunnus(String henkilotunnus) {
        this.henkilotunnus = henkilotunnus;
        return this;
    }

    public ErillishakuRiviBuilder sahkoposti(String sahkoposti) {
        this.sahkoposti = sahkoposti;
        return this;
    }

    public ErillishakuRiviBuilder syntymaAika(String syntymaAika) {
        this.syntymaAika = syntymaAika;
        return this;
    }

    public ErillishakuRiviBuilder sukupuoli(Sukupuoli sukupuoli) {
        this.sukupuoli = sukupuoli;
        return this;
    }

    public ErillishakuRiviBuilder aidinkieli(String aidinkieli) {
        this.aidinkieli = aidinkieli;
        return this;
    }

    public ErillishakuRiviBuilder personOid(String personOid) {
        this.personOid = personOid;
        return this;
    }

    public ErillishakuRiviBuilder hakemuksenTila(String hakemuksenTila) {
        this.hakemuksenTila = hakemuksenTila;
        return this;
    }

    public ErillishakuRiviBuilder ehdollisestiHyvaksyttavissa(boolean ehdollisestiHyvaksyttavissa) {
        this.ehdollisestiHyvaksyttavissa = ehdollisestiHyvaksyttavissa;
        return this;
    }

    public ErillishakuRiviBuilder ehdollisenHyvaksymisenEhtoKoodi(String ehdollisenHyvaksymisenEhtoKoodi) {
        this.ehdollisenHyvaksymisenEhtoKoodi = ehdollisenHyvaksymisenEhtoKoodi;
        return this;
    }

    public ErillishakuRiviBuilder ehdollisenHyvaksymisenEhtoFI(String ehdollisenHyvaksymisenEhtoFI) {
        this.ehdollisenHyvaksymisenEhtoFI = ehdollisenHyvaksymisenEhtoFI;
        return this;
    }

    public ErillishakuRiviBuilder ehdollisenHyvaksymisenEhtoSV(String ehdollisenHyvaksymisenEhtoSV) {
        this.ehdollisenHyvaksymisenEhtoSV = ehdollisenHyvaksymisenEhtoSV;
        return this;
    }

    public ErillishakuRiviBuilder ehdollisenHyvaksymisenEhtoEN(String ehdollisenHyvaksymisenEhtoEN) {
        this.ehdollisenHyvaksymisenEhtoEN = ehdollisenHyvaksymisenEhtoEN;
        return this;
    }

    public ErillishakuRiviBuilder hyvaksymiskirjeLahetetty(Date hyvaksymiskirjeLahetetty) {
        this.hyvaksymiskirjeLahetetty = hyvaksymiskirjeLahetetty;
        return this;
    }

    public ErillishakuRiviBuilder vastaanottoTila(String vastaanottoTila) {
        this.vastaanottoTila = vastaanottoTila;
        return this;
    }

    public ErillishakuRiviBuilder ilmoittautumisTila(String ilmoittautumisTila) {
        this.ilmoittautumisTila = ilmoittautumisTila;
        return this;
    }

    public ErillishakuRiviBuilder julkaistaankoTiedot(boolean julkaistaankoTiedot) {
        this.julkaistaankoTiedot = julkaistaankoTiedot;
        return this;
    }

    public ErillishakuRiviBuilder poistetaankoRivi(boolean poistetaankoRivi) {
        this.poistetaankoRivi = poistetaankoRivi;
        return this;
    }

    public ErillishakuRiviBuilder maksuvelvollisuus(String maksuvelvollisuus) {
        this.maksuvelvollisuus = maksuvelvollisuus;
        return this;
    }

    public ErillishakuRiviBuilder asiointikieli(String asiointikieli) {
        this.asiointikieli = asiointikieli;
        return this;
    }

    public ErillishakuRiviBuilder puhelinnumero(String puhelinnumero) {
        this.puhelinnumero = puhelinnumero;
        return this;
    }

    public ErillishakuRiviBuilder osoite(String osoite) {
        this.osoite = osoite;
        return this;
    }

    public ErillishakuRiviBuilder postinumero(String postinumero) {
        this.postinumero = postinumero;
        return this;
    }

    public ErillishakuRiviBuilder postitoimipaikka(String postitoimipaikka) {
        this.postitoimipaikka = postitoimipaikka;
        return this;
    }

    public ErillishakuRiviBuilder asuinmaa(String asuinmaa) {
        this.asuinmaa = asuinmaa;
        return this;
    }

    public ErillishakuRiviBuilder kansalaisuus(String kansalaisuus) {
        this.kansalaisuus = kansalaisuus;
        return this;
    }

    public ErillishakuRiviBuilder kotikunta(String kotikunta) {
        this.kotikunta = kotikunta;
        return this;
    }

    public ErillishakuRiviBuilder toisenAsteenSuoritus(Boolean toisenAsteenSuoritus) {
        this.toisenAsteenSuoritus = toisenAsteenSuoritus;
        return this;
    }

    public ErillishakuRiviBuilder toisenAsteenSuoritusmaa(String toisenAsteenSuoritusmaa) {
        this.toisenAsteenSuoritusmaa = toisenAsteenSuoritusmaa;
        return this;
    }

    public ErillishakuRivi build() {
        return new ErillishakuRivi(
                hakemusOid,
                sukunimi,
                etunimi,
                henkilotunnus,
                sahkoposti,
                syntymaAika,
                sukupuoli,
                personOid,
                aidinkieli,
                hakemuksenTila,
                ehdollisestiHyvaksyttavissa,
                ehdollisenHyvaksymisenEhtoKoodi,
                ehdollisenHyvaksymisenEhtoFI,
                ehdollisenHyvaksymisenEhtoSV,
                ehdollisenHyvaksymisenEhtoEN,
                hyvaksymiskirjeLahetetty,
                vastaanottoTila,
                ilmoittautumisTila,
                julkaistaankoTiedot,
                poistetaankoRivi,
                asiointikieli, puhelinnumero,
                osoite,
                postinumero,
                postitoimipaikka,
                asuinmaa,
                kansalaisuus,
                kotikunta,
                toisenAsteenSuoritus,
                toisenAsteenSuoritusmaa,
                maksuvelvollisuus
        );
    }
}
