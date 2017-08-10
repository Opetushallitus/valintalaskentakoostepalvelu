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
import static fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiHelper.validateRequiredValue;
import static fi.vm.sade.valinta.kooste.util.HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus;
import static org.apache.commons.lang.StringUtils.isBlank;

public class ErillishaunTuontiValidator {

    private KoodistoCachedAsyncResource koodistoCachedAsyncResource;

    private static final String PHONE_PATTERN = "^$|^([0-9\\(\\)\\/\\+ \\-]*)$";

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
                        if(errors.size() > 0) {
                            poikkeusRivis.add(new ErillishaunDataException.PoikkeusRivi(indeksi,  StringUtils.join(errors, " ") + " : " + rivi));
                        }
                    } else {
                        // validoi poistettavaksi merkitty rivi
                        if (rivi.getHakemusOid() == null) {
                            poikkeusRivis.add(new ErillishaunDataException.PoikkeusRivi(indeksi, "Poistettavaksi merkatulla riville ei löytynyt hakemuksen tunnistetta"));
                        }
                    }
                });
        if(!poikkeusRivis.isEmpty()) {
            throw new ErillishaunDataException(poikkeusRivis);
        }
    }

    protected List<String> validoi(Hakutyyppi tyyppi, ErillishakuRivi rivi, boolean saveApplications) {
        List<String> errors = new ArrayList<>();
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
        // Syntymäaika oikeassa formaatissa
        if(!isBlank(rivi.getSyntymaAika())) {
            try {
                ErillishakuRivi.SYNTYMAAIKAFORMAT.parseDateTime(rivi.getSyntymaAika());
            } catch(Exception e){
                errors.add("Syntymäaika '" + rivi.getSyntymaAika() + "' on väärin muotoiltu (syötettävä muodossa pp.mm.vvvv).");
            }
        }
        // Jos vahvatunniste puuttuu niin nimet on pakollisia tietoja
        if(isBlank(rivi.getPersonOid())) {
            if (isBlank(rivi.getEtunimi()) || isBlank(rivi.getSukunimi())) {
                errors.add("Etunimi ja sukunimi on pakollisia.");
            }
        }
        // Henkilötunnus on oikeassa formaatissa jos sellainen on syötetty
        if(!isBlank(rivi.getHenkilotunnus()) && !tarkistaHenkilotunnus(rivi.getHenkilotunnus())) {
            errors.add("Henkilötunnus ("+rivi.getHenkilotunnus()+") on virheellinen.");
        }
        if (!rivi.isJulkaistaankoTiedot()
                && !(ValintatuloksenTila.KESKEN.name().equals(rivi.getVastaanottoTila())
                || ValintatuloksenTila.OTTANUT_VASTAAN_TOISEN_PAIKAN.name().equals(rivi.getVastaanottoTila())
                || StringUtils.isEmpty(rivi.getVastaanottoTila()))) {

            errors.add("Vastaanottotietoa ei voi päivittää jos valinta ei ole julkaistavissa tai vastaanottotieto ei ole kesken");
        }

        if(!rivi.getHakemuksenTila().equals("KESKEN") && hakemuksenTila(rivi) == null) {
            errors.add("Annettu HakemuksenTila ei ole sallittu arvo. ("+ rivi.getHakemuksenTila() + ")");
        }
        if(!rivi.getIlmoittautumisTila().isEmpty() && ilmoittautumisTila(rivi) == null) {
            errors.add("Ilmoittautumistilan tulee olla joko tyhjä tai jokin hyväksytyistä arvoista. ("+rivi.getIlmoittautumisTila()+")" );
        }
        if(!rivi.getVastaanottoTila().isEmpty() && valintatuloksenTila(rivi) == null) {
            errors.add("Vastaanottotilan tulee olla joko tyhjä tai jokin hyväksytyistä arvoista. ("+rivi.getVastaanottoTila()+")" );
        }

        if (!"KESKEN".equalsIgnoreCase(rivi.getHakemuksenTila())) {
            ValintatuloksenTila vt = valintatuloksenTila(rivi);
            String tilaVirhe = ValidoiTilatUtil.validoi(hakemuksenTila(rivi), vt, ilmoittautumisTila(rivi));
            if (tilaVirhe != null) {
                errors.add(tilaVirhe + ".");
            }
        }
        if((isBlank(rivi.getPersonOid()) && isBlank(rivi.getHenkilotunnus())) && Sukupuoli.EI_SUKUPUOLTA.equals(rivi.getSukupuoli())) {
            errors.add("Sukupuoli ("+rivi.getSukupuoli()+") on pakollinen kun henkilötunnus ja personOID puuttuu.");
        }

        if (isBlank(rivi.getHenkilotunnus()) &&
                isBlank(rivi.getPersonOid()) &&
                StringUtils.trimToEmpty(rivi.getAidinkieli()).isEmpty()) {
            errors.add("Äidinkieli on pakollinen tieto, kun henkilötunnus ja henkilö OID puuttuvat.");
        }

        Map<String, Koodi> kieliKoodit = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.KIELI);
        if (! StringUtils.trimToEmpty(rivi.getAidinkieli()).isEmpty() &&
                ! kieliKoodit.keySet().contains(rivi.getAidinkieli().toUpperCase())) {
            errors.add("Äidinkielen kielikoodi ("+rivi.getAidinkieli()+") on virheellinen.");
        }

        if (!isBlank(rivi.getAsiointikieli()) && !ErillishakuDataRivi.ASIONTIKIELEN_ARVOT.contains(StringUtils.trimToEmpty(rivi.getAsiointikieli()).toLowerCase())) {
            errors.add("Asiointikieli (" + rivi.getAsiointikieli() + ") on virheellinen (sallitut arvot ["+
                    StringUtils.join(ErillishakuDataRivi.ASIONTIKIELEN_ARVOT, '|') +
                    "]).");
        }


        Map<String, Koodi> maaKoodit = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
        String asuinmaa = StringUtils.trimToEmpty(rivi.getAsuinmaa()).toUpperCase();
        if (!asuinmaa.isEmpty() && !maaKoodit.keySet().contains(asuinmaa)) {
            errors.add("Asuinmaan maakoodi (" +  rivi.getAsuinmaa() + ") on virheellinen.");
        }

        String kansalaisuus = StringUtils.trimToEmpty(rivi.getKansalaisuus()).toUpperCase();
        if (! kansalaisuus.isEmpty() && !maaKoodit.keySet().contains(kansalaisuus)) {
            errors.add("Kansalaisuuden maakoodi (" + rivi.getKansalaisuus() + ") on virheellinen.");
        }

        String toisenAsteenSuoritusmaa = StringUtils.trimToEmpty(rivi.getToisenAsteenSuoritusmaa()).toUpperCase();
        if (! toisenAsteenSuoritusmaa.isEmpty() && !maaKoodit.keySet().contains(toisenAsteenSuoritusmaa)) {
            errors.add("Toisen asteen pohjakoulutuksen suoritusmaan maakoodi (" + rivi.getToisenAsteenSuoritusmaa() + ") on virheellinen.");
        }

        String kotikunta = StringUtils.trimToEmpty(rivi.getKotikunta());
        if(!kotikunta.isEmpty()) {
            if (convertKuntaNimiToKuntaKoodi(kotikunta) == null) {
                errors.add("Virheellinen kotikunta (" + rivi.getKotikunta() + ").");
            }
        }

        if (asuinmaa.equals(OsoiteHakemukseltaUtil.SUOMI)) {
            Map<String, Koodi> postiKoodit = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI);
            String postinumero = StringUtils.trimToEmpty(rivi.getPostinumero());
            if (!postinumero.isEmpty() && !postiKoodit.keySet().contains(postinumero)) {
                errors.add("Virheellinen suomalainen postinumero (" + rivi.getPostinumero() + ").");
            }

            String postitoimipaikka = StringUtils.trimToEmpty(rivi.getPostitoimipaikka()).toUpperCase();

            if(!postitoimipaikka.isEmpty()) {
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

        String puhelinnumero = StringUtils.trimToEmpty(rivi.getPuhelinnumero());
        if (! puhelinnumero.isEmpty() && !puhelinnumero.matches(PHONE_PATTERN)) {
            errors.add("Virheellinen puhelinnumero (" + rivi.getPuhelinnumero() + ").");
        }

        if (saveApplications && tyyppi == Hakutyyppi.KORKEAKOULU) {
            validateRequiredValue(asuinmaa, "asuinmaa", errors);
            validateRequiredValue(kansalaisuus, "kansalaisuus", errors);
            validateRequiredValue(kotikunta, "kotikunta", errors);

            Boolean toisenAsteenSuoritus = rivi.getToisenAsteenSuoritus();
            validateRequiredValue(ErillishakuDataRivi.getTotuusarvoString(toisenAsteenSuoritus), "toisen asteen suoritus", errors);
            if(BooleanUtils.isTrue(toisenAsteenSuoritus)) {
                validateRequiredValue(toisenAsteenSuoritusmaa, "toisen asteen pohjakoulutuksen maa", errors);
            } else if(StringUtils.isNotBlank(toisenAsteenSuoritusmaa)) {
                errors.add("Toisen asteen pohjakoulutuksen suoritusmaata (" + rivi.getToisenAsteenSuoritusmaa() + ") ei saa antaa, jos ei toisen asteen pohjakoulutusta ole suoritettu.");
            }
        }

        /*
        //Tämä on toimiva validaatio, mutta sitä kannattaa käyttää vasta kun vaihtoehdot ehdolliselle hyväksymiselle saadaan valittua excelin pudotusvalikosta.
        if(rivi.getEhdollisestiHyvaksyttavissa()) {
            Map<String, Koodi> ehdot = koodistoCachedAsyncResource.haeKoodisto("hyvaksynnanehdot");
            if(!ehdot.isEmpty()) {
                Boolean sallittuehto = ehdot.values().stream()
                        .flatMap(x -> x.getMetadata().stream())
                        .map(Metadata::getNimi)
                        .anyMatch(x -> x.equalsIgnoreCase(rivi.getEhdollisenHyvaksymisenEhtoKoodi()));
                if (!sallittuehto)
                    errors.add("Jos ehdollinen hyväksyntä on aktiivinen, on hyväksymisen ehdon oltava jokin pudotusvalikon arvoista.");
            }
        }
        */

        if (rivi.getEhdollisestiHyvaksyttavissa() && rivi.getEhdollisenHyvaksymisenEhtoKoodi() != null &&
                rivi.getEhdollisenHyvaksymisenEhtoKoodi().equals(EhdollisenHyvaksymisenEhtoKoodi.EHTO_MUU)) {
            if (StringUtils.isEmpty(rivi.getEhdollisenHyvaksymisenEhtoFI())) errors.add("Ehdollisen hyväksynnän ehto FI -kenttä oli tyhjä");
            if (StringUtils.isEmpty(rivi.getEhdollisenHyvaksymisenEhtoSV())) errors.add("Ehdollisen hyväksynnän ehto SV -kenttä oli tyhjä");
            if (StringUtils.isEmpty(rivi.getEhdollisenHyvaksymisenEhtoEN())) errors.add("Ehdollisen hyväksynnän ehto EN -kenttä oli tyhjä");
        }

        return errors;
    }

}
