package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import com.google.common.collect.Lists;
import fi.vm.sade.sijoittelu.domain.EhdollisenHyvaksymisenEhtoKoodi;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuDataRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.Sukupuoli;
import fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource;
import fi.vm.sade.valinta.kooste.erillishaku.util.ValidoiTilatUtil;
import fi.vm.sade.valinta.kooste.exception.ErillishaunDataException;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Metadata;
import fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.codepoetics.protonpack.StreamUtils.zipWithIndex;
import static fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiHelper.*;
import static fi.vm.sade.valinta.kooste.util.HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus;
import static org.apache.commons.lang.StringUtils.isBlank;

public class ErillishaunTuontiValidator {

    private static final String PHONE_PATTERN = "^$|^([0-9\\(\\)\\/\\+ \\-]*)$";
    private KoodistoCachedAsyncResource koodistoCachedAsyncResource;

    public ErillishaunTuontiValidator(KoodistoCachedAsyncResource koodistoCachedAsyncResource) {
        this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
    }

    protected String convertKuntaNimiToKuntaKoodi(String nimi) {
        Map<String, Koodi> kuntaKoodit = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.KUNTA);
        return kuntaKoodit.values().stream().flatMap(koodi -> koodi.getMetadata().stream().map(metadata -> new ImmutablePair<>(metadata.getNimi(), koodi.getKoodiArvo())))
                .filter(x -> x.getLeft().equalsIgnoreCase(nimi))
                .map(ImmutablePair::getRight)
                .findFirst()
                .orElse(null);
    }

    protected String convertKansalaisuusKoodi(String kansalaisuus) {
        Map<String, Koodi> maaKoodit1 = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
        Map<String, Koodi> maaKoodit2 = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_2);

        String maaNimi = KoodistoCachedAsyncResource.haeKoodistaArvo(maaKoodit1.get(kansalaisuus), "FI", null);
        return maaKoodit2.values().stream().flatMap(koodi -> koodi.getMetadata().stream().map(metadata -> new ImmutablePair<>(metadata.getNimi(), koodi.getKoodiArvo())))
                .filter(x -> x.getLeft().equalsIgnoreCase(maaNimi))
                .map(ImmutablePair::getRight)
                .findFirst()
                .orElse(null);
    }

    protected void validoiRivit(final KirjeProsessi prosessi, final ErillishakuDTO haku, final List<ErillishakuRivi> rivit, final boolean saveApplications) {
        if (rivit.isEmpty()) {
            prosessi.keskeyta(ErillishakuResource.POIKKEUS_TYHJA_DATAJOUKKO);
            throw new RuntimeException(ErillishakuResource.POIKKEUS_TYHJA_DATAJOUKKO);
        }

        Collection<ErillishaunDataException.PoikkeusRivi> poikkeusRivis = Lists.newArrayList();
        zipWithIndex(
                rivit.stream().map(rivi -> {
                    // AUTOTAYTTO VA
                    rivi.getHakemuksenTila();
                    return rivi;
                }))
                .forEach(riviJaIndeksi -> {
                    int indeksi = ((int) riviJaIndeksi.getIndex()) + 1;
                    ErillishakuRivi rivi = riviJaIndeksi.getValue();

                    if (!rivi.isPoistetaankoRivi()) {
                        List<String> errors = validoi(haku.getHakutyyppi(), rivi, saveApplications);
                        if (errors.size() > 0) {
                            poikkeusRivis.add(new ErillishaunDataException.PoikkeusRivi(indeksi, StringUtils.join(errors, " ") + " : " + rivi));
                        }
                    } else {
                        // validoi poistettavaksi merkitty rivi
                        if (rivi.getHakemusOid() == null) {
                            poikkeusRivis.add(new ErillishaunDataException.PoikkeusRivi(indeksi, "Poistettavaksi merkatulla riville ei löytynyt hakemuksen tunnistetta"));
                        }
                    }
                });
        if (!poikkeusRivis.isEmpty()) {
            throw new ErillishaunDataException(poikkeusRivis);
        }
    }

    protected List<String> validoi(Hakutyyppi tyyppi, ErillishakuRivi rivi, boolean saveApplications) {
        List<String> errors = new ArrayList<>();
        validateTunniste(rivi, errors);
        validateSyntymaAika(rivi, errors);
        validateNimet(rivi, errors);
        validateHetu(rivi, errors);
        validateJulkaistavuus(rivi, errors);
        validateTila(rivi, errors);
        validateIlmoittautumisTila(rivi, errors);
        validateVastaanottoTila(rivi, errors);
        validateValintatuloksenTila(rivi, errors);
        validateSukupuoli(rivi, errors);
        validateAidinkieli(rivi, errors);
        validateKielikoodit(rivi, errors);
        validateAsiointikieli(rivi, errors);

        Map<String, Koodi> maaKoodit = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
        String asuinmaa = validateAsuinmaa(rivi, errors, maaKoodit);
        String kansalaisuus = validateKansalaisuus(rivi, errors, maaKoodit);
        String toisenAsteenSuoritusmaa = validateToisenAsteenSuoritusmaa(rivi, errors, maaKoodit);
        String kotikunta = validateKotikunta(rivi, errors);
        validatePostinumeroAndToimipaikka(rivi, errors, asuinmaa);
        validatePuhelinnumero(rivi, errors);
        if (saveApplications && tyyppi == Hakutyyppi.KORKEAKOULU) {
            validateKorkeakoulu(rivi, errors, asuinmaa, kansalaisuus, toisenAsteenSuoritusmaa, kotikunta);
        }
        //Tämä on toimiva validaatio, mutta sitä kannattaa käyttää vasta kun vaihtoehdot ehdolliselle hyväksymiselle saadaan valittua excelin pudotusvalikosta.

        //Since we now have dropdown with codes & explanations, we'll split the ehdollisenHyvaksymisenEhtoKoodi and just take the code part:
        if (rivi.getEhdollisenHyvaksymisenEhtoKoodi() != null) {
            String[] splitCode = rivi.getEhdollisenHyvaksymisenEhtoKoodi().split("\\s+");
            rivi.setEhdollisenHyvaksymisenEhtoKoodi(splitCode[0]);
        }

        validateEhdollinenHyvaksynta(rivi, errors);
        validateEhdollisenHyvaksynnanKoodi(rivi, errors);

        return errors;
    }


    private void validateSyntymaAika(ErillishakuRivi rivi, List<String> errors) {
        // Syntymäaika oikeassa formaatissa
        if (!isBlank(rivi.getSyntymaAika())) {
            try {
                ErillishakuRivi.SYNTYMAAIKAFORMAT.parse(rivi.getSyntymaAika());
            } catch(Exception e) {
                errors.add("Syntymäaika '" + rivi.getSyntymaAika() + "' on väärin muotoiltu (syötettävä muodossa pp.mm.vvvv).");
            }
        }
    }

    private void validateNimet(ErillishakuRivi rivi, List<String> errors) {
        // Jos vahvatunniste puuttuu niin nimet on pakollisia tietoja
        if (isBlank(rivi.getPersonOid())) {
            if (isBlank(rivi.getEtunimi()) || isBlank(rivi.getSukunimi())) {
                errors.add("Etunimi ja sukunimi on pakollisia.");
            }
        }
    }

    private void validateTunniste(ErillishakuRivi rivi, List<String> errors) {
        // Yksilöinti onnistuu, eli joku kolmesta löytyy: henkilötunnus,syntymäaika+sukupuoli,henkilö-oid
        if (// mikään seuraavista ei ole totta:
                !(// on syntymaika+sukupuoli tunnistus
                        (!isBlank(rivi.getSyntymaAika())
                                && !Sukupuoli.EI_SUKUPUOLTA.equals(rivi.getSukupuoli()))
                                || // on henkilotunnus
                                !isBlank(rivi.getHenkilotunnus()) ||
                                // on henkilo OID
                                !isBlank(rivi.getPersonOid()))) {
            errors.add("Henkilötunnus, syntymäaika + sukupuoli ja henkilö-oid olivat tyhjiä (vähintään yksi tunniste on syötettävä).");
        }
    }

    private void validateHetu(ErillishakuRivi rivi, List<String> errors) {
        // Henkilötunnus on oikeassa formaatissa jos sellainen on syötetty
        if (!isBlank(rivi.getHenkilotunnus()) && !tarkistaHenkilotunnus(rivi.getHenkilotunnus())) {
            errors.add("Henkilötunnus (" + rivi.getHenkilotunnus() + ") on virheellinen.");
        }
    }

    private void validateJulkaistavuus(ErillishakuRivi rivi, List<String> errors) {
        if (!rivi.isJulkaistaankoTiedot()
                && !(ValintatuloksenTila.KESKEN.name().equals(rivi.getVastaanottoTila())
                || ValintatuloksenTila.OTTANUT_VASTAAN_TOISEN_PAIKAN.name().equals(rivi.getVastaanottoTila())
                || StringUtils.isEmpty(rivi.getVastaanottoTila()))) {

            errors.add("Vastaanottotietoa ei voi päivittää jos valinta ei ole julkaistavissa tai vastaanottotieto ei ole kesken");
        }
    }

    private void validateTila(ErillishakuRivi rivi, List<String> errors) {
        if (!rivi.getHakemuksenTila().equals("KESKEN") && hakemuksenTila(rivi) == null) {
            errors.add("Annettu HakemuksenTila ei ole sallittu arvo. (" + rivi.getHakemuksenTila() + ")");
        }
    }

    private void validateIlmoittautumisTila(ErillishakuRivi rivi, List<String> errors) {
        if (!rivi.getIlmoittautumisTila().isEmpty() && ilmoittautumisTila(rivi) == null) {
            errors.add("Ilmoittautumistilan tulee olla joko tyhjä tai jokin hyväksytyistä arvoista. (" + rivi.getIlmoittautumisTila() + ")");
        }
    }

    private void validateVastaanottoTila(ErillishakuRivi rivi, List<String> errors) {
        if (!rivi.getVastaanottoTila().isEmpty() && valintatuloksenTila(rivi) == null) {
            errors.add("Vastaanottotilan tulee olla joko tyhjä tai jokin hyväksytyistä arvoista. (" + rivi.getVastaanottoTila() + ")");
        }
    }

    private void validateValintatuloksenTila(ErillishakuRivi rivi, List<String> errors) {
        if (!"KESKEN".equalsIgnoreCase(rivi.getHakemuksenTila())) {
            ValintatuloksenTila vt = valintatuloksenTila(rivi);
            String tilaVirhe = ValidoiTilatUtil.validoi(hakemuksenTila(rivi), vt, ilmoittautumisTila(rivi));
            if (tilaVirhe != null) {
                errors.add(tilaVirhe + ".");
            }
        }
    }


    private void validateSukupuoli(ErillishakuRivi rivi, List<String> errors) {
        if ((isBlank(rivi.getPersonOid()) && isBlank(rivi.getHenkilotunnus())) && Sukupuoli.EI_SUKUPUOLTA.equals(rivi.getSukupuoli())) {
            errors.add("Sukupuoli (" + rivi.getSukupuoli() + ") on pakollinen kun henkilötunnus ja personOID puuttuu.");
        }
    }


    private void validateAidinkieli(ErillishakuRivi rivi, List<String> errors) {
        if (isBlank(rivi.getHenkilotunnus()) &&
                isBlank(rivi.getPersonOid()) &&
                StringUtils.trimToEmpty(rivi.getAidinkieli()).isEmpty()) {
            errors.add("Äidinkieli on pakollinen tieto, kun henkilötunnus ja henkilö OID puuttuvat.");
        }
    }


    private void validateKielikoodit(ErillishakuRivi rivi, List<String> errors) {
        Map<String, Koodi> kieliKoodit = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.KIELI);
        if (!StringUtils.trimToEmpty(rivi.getAidinkieli()).isEmpty() &&
                !kieliKoodit.keySet().contains(rivi.getAidinkieli().toUpperCase())) {
            errors.add("Äidinkielen kielikoodi (" + rivi.getAidinkieli() + ") on virheellinen.");
        }
    }


    private void validateAsiointikieli(ErillishakuRivi rivi, List<String> errors) {
        if (!isBlank(rivi.getAsiointikieli()) && !ErillishakuDataRivi.ASIONTIKIELEN_ARVOT.contains(StringUtils.trimToEmpty(rivi.getAsiointikieli()).toLowerCase())) {
            errors.add("Asiointikieli (" + rivi.getAsiointikieli() + ") on virheellinen (sallitut arvot [" +
                    StringUtils.join(ErillishakuDataRivi.ASIONTIKIELEN_ARVOT, '|') +
                    "]).");
        }
    }


    private String validateAsuinmaa(ErillishakuRivi rivi, List<String> errors, Map<String, Koodi> maaKoodit) {
        String asuinmaa = StringUtils.trimToEmpty(rivi.getAsuinmaa()).toUpperCase();
        if (!asuinmaa.isEmpty() && !maaKoodit.keySet().contains(asuinmaa)) {
            errors.add("Asuinmaan maakoodi (" + rivi.getAsuinmaa() + ") on virheellinen.");
        }
        return asuinmaa;
    }

    private String validateKansalaisuus(ErillishakuRivi rivi, List<String> errors, Map<String, Koodi> maaKoodit) {
        String kansalaisuus = StringUtils.trimToEmpty(rivi.getKansalaisuus()).toUpperCase();
        if (!kansalaisuus.isEmpty() && !maaKoodit.keySet().contains(kansalaisuus)) {
            errors.add("Kansalaisuuden maakoodi (" + rivi.getKansalaisuus() + ") on virheellinen.");
        }
        return kansalaisuus;
    }


    private String validateToisenAsteenSuoritusmaa(ErillishakuRivi rivi, List<String> errors, Map<String, Koodi> maaKoodit) {
        String toisenAsteenSuoritusmaa = StringUtils.trimToEmpty(rivi.getToisenAsteenSuoritusmaa()).toUpperCase();
        if (!toisenAsteenSuoritusmaa.isEmpty() && !maaKoodit.keySet().contains(toisenAsteenSuoritusmaa)) {
            errors.add("Toisen asteen pohjakoulutuksen suoritusmaan maakoodi (" + rivi.getToisenAsteenSuoritusmaa() + ") on virheellinen.");
        }
        return toisenAsteenSuoritusmaa;
    }

    private String validateKotikunta(ErillishakuRivi rivi, List<String> errors) {
        String kotikunta = StringUtils.trimToEmpty(rivi.getKotikunta());
        if (!kotikunta.isEmpty()) {
            if (convertKuntaNimiToKuntaKoodi(kotikunta) == null) {
                errors.add("Virheellinen kotikunta (" + rivi.getKotikunta() + ").");
            }
        }
        return kotikunta;
    }


    private void validatePostinumeroAndToimipaikka(ErillishakuRivi rivi, List<String> errors, String asuinmaa) {
        if (asuinmaa.equals(OsoiteHakemukseltaUtil.SUOMI)) {
            Map<String, Koodi> postiKoodit = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI);
            String postinumero = StringUtils.trimToEmpty(rivi.getPostinumero());
            if (!postinumero.isEmpty() && !postiKoodit.keySet().contains(postinumero)) {
                errors.add("Virheellinen suomalainen postinumero (" + rivi.getPostinumero() + ").");
            }

            String postitoimipaikka = StringUtils.trimToEmpty(rivi.getPostitoimipaikka()).toUpperCase();

            if (!postitoimipaikka.isEmpty()) {
                boolean postitoimipaikkaKoodistossa = postiKoodit.values().stream()
                        .flatMap(x -> x.getMetadata().stream())
                        .map(Metadata::getNimi)
                        .anyMatch(x -> x.equalsIgnoreCase(postitoimipaikka));
                if (!postitoimipaikkaKoodistossa) {
                    errors.add("Virheellinen suomalainen postitoimipaikka (" + rivi.getPostinumero() + ").");
                }

                if (!postinumero.isEmpty() && postiKoodit.containsKey(postinumero) &&
                        !postiKoodit.get(postinumero).getMetadata().stream().anyMatch(m -> m.getNimi().equalsIgnoreCase(postitoimipaikka))) {
                    errors.add("Annettu suomalainen postinumero (" + rivi.getPostinumero() + ") ei vastaa annettua postitoimipaikkaa ("
                            + rivi.getPostitoimipaikka() + ").");
                }
            }
        }
    }


    private void validatePuhelinnumero(ErillishakuRivi rivi, List<String> errors) {
        String puhelinnumero = StringUtils.trimToEmpty(rivi.getPuhelinnumero());
        if (!puhelinnumero.isEmpty() && !puhelinnumero.matches(PHONE_PATTERN)) {
            errors.add("Virheellinen puhelinnumero (" + rivi.getPuhelinnumero() + ").");
        }
    }


    private void validateKorkeakoulu(ErillishakuRivi rivi, List<String> errors, String asuinmaa, String kansalaisuus, String toisenAsteenSuoritusmaa, String kotikunta) {
        validateRequiredValue(asuinmaa, "asuinmaa", errors);
        validateRequiredValue(kansalaisuus, "kansalaisuus", errors);
        validateRequiredValue(kotikunta, "kotikunta", errors);

        Boolean toisenAsteenSuoritus = rivi.getToisenAsteenSuoritus();
        validateRequiredValue(ErillishakuDataRivi.getTotuusarvoString(toisenAsteenSuoritus), "toisen asteen suoritus", errors);
        if (BooleanUtils.isTrue(toisenAsteenSuoritus)) {
            validateRequiredValue(toisenAsteenSuoritusmaa, "toisen asteen pohjakoulutuksen maa", errors);
        } else if (StringUtils.isNotBlank(toisenAsteenSuoritusmaa)) {
            errors.add("Toisen asteen pohjakoulutuksen suoritusmaata (" + rivi.getToisenAsteenSuoritusmaa() + ") ei saa antaa, jos ei toisen asteen pohjakoulutusta ole suoritettu.");
        }
    }


    @SuppressWarnings("unused")
    private void validateEhdollinenHyvaksynta(ErillishakuRivi rivi, List<String> errors) {
        if (rivi.getEhdollisestiHyvaksyttavissa()) {
            Map<String, Koodi> ehdot = koodistoCachedAsyncResource.haeKoodisto("hyvaksynnanehdot");
            if (!ehdot.isEmpty()) {
                Boolean sallittuehto = ehdot.containsKey(rivi.getEhdollisenHyvaksymisenEhtoKoodi());
                if (!sallittuehto)
                    errors.add("Jos ehdollinen hyväksyntä on aktiivinen, on hyväksymisen ehdon oltava jokin pudotusvalikon arvoista.");
            }
        }
    }


    private void validateEhdollisenHyvaksynnanKoodi(ErillishakuRivi rivi, List<String> errors) {
        if (rivi.getEhdollisestiHyvaksyttavissa() && rivi.getEhdollisenHyvaksymisenEhtoKoodi() != null &&
                rivi.getEhdollisenHyvaksymisenEhtoKoodi().equals(EhdollisenHyvaksymisenEhtoKoodi.EHTO_MUU)) {
            if (StringUtils.isEmpty(rivi.getEhdollisenHyvaksymisenEhtoFI()))
                errors.add("Ehdollisen hyväksynnän ehto FI -kenttä oli tyhjä");
            if (StringUtils.isEmpty(rivi.getEhdollisenHyvaksymisenEhtoSV()))
                errors.add("Ehdollisen hyväksynnän ehto SV -kenttä oli tyhjä");
            if (StringUtils.isEmpty(rivi.getEhdollisenHyvaksymisenEhtoEN()))
                errors.add("Ehdollisen hyväksynnän ehto EN -kenttä oli tyhjä");
        }
    }

}
