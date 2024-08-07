package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import static java.util.Optional.of;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import com.google.common.collect.Lists;
import fi.vm.sade.sijoittelu.domain.HakemuksenTila;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRiviBuilder;
import fi.vm.sade.valinta.kooste.erillishaku.excel.Sukupuoli;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.KielisyysDto;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErillishaunTuontiHelper {

  private static final Logger LOG = LoggerFactory.getLogger(ErillishaunTuontiHelper.class);

  public static final List<HakemuksenTila> VAIN_HAKEMUKSENTILALLISET_TILAT =
      Arrays.asList(HakemuksenTila.HYLATTY, HakemuksenTila.VARALLA, HakemuksenTila.PERUUNTUNUT);

  public static boolean isUusi(ErillishakuRivi rivi) {
    return StringUtils.isEmpty(rivi.getVastaanottoTila())
        && StringUtils.isEmpty(rivi.getIlmoittautumisTila());
  }

  public static boolean isKesken(ErillishakuRivi rivi) {
    return "KESKEN".equalsIgnoreCase(rivi.getHakemuksenTila());
  }

  public static List<ErillishakuRivi> autoTaytto(final List<ErillishakuRivi> rivit) {
    // jos hakemuksentila on hylatty, varalla, peruuntunut tai kesken niin autotaytetaan loput tilat
    // KESKEN, EI_TEHTY

    return rivit.stream()
        .map(
            rivi -> {
              if ((VAIN_HAKEMUKSENTILALLISET_TILAT.contains(hakemuksenTila(rivi))
                      && !isUusi(rivi)
                      && !ValintatuloksenTila.OTTANUT_VASTAAN_TOISEN_PAIKAN
                          .name()
                          .equals(rivi.getVastaanottoTila()))
                  || isKesken(rivi)) {
                return ErillishakuRiviBuilder.fromRivi(rivi)
                    .vastaanottoTila("KESKEN")
                    .ilmoittautumisTila("EI_TEHTY")
                    .build();
              } else {
                return rivi;
              }
            })
        .collect(Collectors.toList());
  }

  public static HakemuksenTila hakemuksenTila(ErillishakuRivi rivi) {
    return nullIfFails(() -> HakemuksenTila.valueOf(rivi.getHakemuksenTila()));
  }

  public static IlmoittautumisTila ilmoittautumisTila(ErillishakuRivi rivi) {
    return nullIfFails(() -> IlmoittautumisTila.valueOf(rivi.getIlmoittautumisTila()));
  }

  public static ValintatuloksenTila valintatuloksenTila(ErillishakuRivi rivi) {
    return nullIfFails(() -> ValintatuloksenTila.valueOf(rivi.getVastaanottoTila()));
  }

  public static <T> T nullIfFails(Supplier<T> lambda) {
    try {
      return lambda.get();
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public static void validateRequiredValue(String value, String name, List<String> errors) {
    if (StringUtils.isBlank(value)) {
      errors.add("Pakollinen tieto \"" + name + "\" puuttuu.");
    }
  }

  public static class HenkilonRivinPaattelyEpaonnistuiException extends RuntimeException {
    private HenkilonRivinPaattelyEpaonnistuiException(String message) {
      super(message);
    }
  }

  public static ErillishakuRivi etsiHenkiloaVastaavaRivi(
      HenkiloPerustietoDto henkilo, List<ErillishakuRivi> kaikkiLisattavatTaiKeskeneraiset) {
    Optional<ErillishakuRivi> riviOidinMukaan =
        kaikkiLisattavatTaiKeskeneraiset.stream()
            .filter(
                r ->
                    StringUtils.isNotBlank(r.getPersonOid())
                        && r.getPersonOid().equals(henkilo.getOidHenkilo()))
            .findFirst();
    if (riviOidinMukaan.isPresent()) {
      return riviOidinMukaan.get();
    }
    Optional<ErillishakuRivi> riviHetunMukaan =
        kaikkiLisattavatTaiKeskeneraiset.stream()
            .filter(
                r ->
                    StringUtils.isNotBlank(r.getHenkilotunnus())
                        && r.getHenkilotunnus().equals(henkilo.getHetu()))
            .findFirst();
    if (riviHetunMukaan.isPresent()) {
      ErillishakuRivi loytynyt = riviHetunMukaan.get();
      if (StringUtils.isNotBlank(loytynyt.getPersonOid())
          && StringUtils.isNotBlank(henkilo.getOidHenkilo())
          && !loytynyt.getPersonOid().equals(henkilo.getOidHenkilo())) {
        LOG.warn(
            String.format(
                "Henkilölle %s hetun mukaan löytyneellä rivillä %s on eri henkilöoid (%s vs %s)",
                henkilo, loytynyt, henkilo.getOidHenkilo(), loytynyt.getPersonOid()));
      }
      return loytynyt;
    }
    Optional<ErillishakuRivi> riviSyntymaajanSukupuolenJaNimenMukaan =
        kaikkiLisattavatTaiKeskeneraiset.stream()
            .filter(r -> r.formatSyntymaAikaAsDate() != null)
            .filter(r -> r.getSukupuoli() != null && henkilo.getSukupuoli() != null)
            .filter(r -> r.getEtunimi() != null && henkilo.getEtunimet() != null)
            .filter(r -> r.getSukunimi() != null && henkilo.getSukunimi() != null)
            .filter(
                r ->
                    AtaruHakemusPrototyyppi.parseDate(r.formatSyntymaAikaAsDate())
                            .equals(AtaruHakemusPrototyyppi.parseDate(henkilo.getSyntymaaika()))
                        && r.getSukupuoli()
                            .equals(Sukupuoli.toSukupuoliEnum(henkilo.getSukupuoli()))
                        && r.getEtunimi().equals(henkilo.getEtunimet())
                        && r.getSukunimi().equals(henkilo.getSukunimi()))
            .findFirst();
    if (riviSyntymaajanSukupuolenJaNimenMukaan.isPresent()) {
      return riviSyntymaajanSukupuolenJaNimenMukaan.get();
    }
    throw new HenkilonRivinPaattelyEpaonnistuiException(
        "Ei löytynyt "
            + kaikkiLisattavatTaiKeskeneraiset.size()
            + " tuodusta rivistä henkilöä "
            + henkilo);
  }

  public static ErillishakuRivi riviWithHenkiloData(
      HenkiloPerustietoDto henkilo, List<ErillishakuRivi> kaikkiLisattavatTaiKeskeneraiset) {
    ErillishakuRivi rivi = etsiHenkiloaVastaavaRivi(henkilo, kaikkiLisattavatTaiKeskeneraiset);
    return riviWithHenkiloData(henkilo, rivi);
  }

  /**
   * @implNote Uses {@link HenkiloPerustietoDto#kutsumanimi} to populate {@link
   *     ErillishakuRivi#etunimi}
   */
  public static ErillishakuRivi riviWithHenkiloData(
      HenkiloPerustietoDto henkilo, ErillishakuRivi rivi) {
    String aidinkieli = kielisyysToString(henkilo.getAidinkieli());
    String asiointikieli = kielisyysToString(henkilo.getAsiointiKieli());
    String sukupuoli = henkilo.getSukupuoli();
    return ErillishakuRiviBuilder.fromRivi(rivi)
        .sukunimi(henkilo.getSukunimi())
        .etunimi(henkilo.getEtunimet())
        .henkilotunnus(henkilo.getHetu())
        .sahkoposti(StringUtils.trimToEmpty(rivi.getSahkoposti()))
        .syntymaAika(AtaruHakemusPrototyyppi.parseDate(henkilo.getSyntymaaika()))
        .sukupuoli(
            isNotBlank(sukupuoli) ? Sukupuoli.toSukupuoliEnum(sukupuoli) : rivi.getSukupuoli())
        .personOid(henkilo.getOidHenkilo())
        .aidinkieli(isNotBlank(aidinkieli) ? aidinkieli : rivi.getAidinkieli())
        .asiointikieli(isNotBlank(asiointikieli) ? asiointikieli : rivi.getAsiointikieli())
        .build();
  }

  private static String toAtaruAsiointikieli(String asiointikieli) {
    if (asiointikieli.equalsIgnoreCase("fi")) {
      return "1";
    } else if (asiointikieli.equalsIgnoreCase("sv")) {
      return "2";
    } else if (asiointikieli.equalsIgnoreCase("en")) {
      return "3";
    }

    return "1";
  }

  private static String convertMaakoodi(
      String maakoodi, KoodistoCachedAsyncResource koodistoCachedAsyncResource) {
    Map<String, Koodi> maaKoodit1 =
        koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
    Map<String, Koodi> maaKoodit2 =
        koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_2);

    String maaNimi =
        koodistoCachedAsyncResource.haeKoodistaArvo(maaKoodit1.get(maakoodi), "FI", null);
    return maaKoodit2.values().stream()
        .flatMap(
            koodi ->
                koodi.getMetadata().stream()
                    .map(metadata -> new ImmutablePair<>(metadata.getNimi(), koodi.getKoodiArvo())))
        .filter(x -> x.getLeft().equalsIgnoreCase(maaNimi))
        .map(ImmutablePair::getRight)
        .findFirst()
        .orElse(null);
  }

  public static AtaruHakemusPrototyyppi createHakemusprototyyppi(
      ErillishakuRivi rivi,
      String kotikunta,
      String hakuOid,
      String hakukohdeOid,
      KoodistoCachedAsyncResource koodistoCachedAsyncResource) {
    AtaruHakemusPrototyyppi hakemus = new AtaruHakemusPrototyyppi();
    hakemus.setHakuOid(hakuOid);
    hakemus.setHakukohdeOid(hakukohdeOid);
    hakemus.setAidinkieli(rivi.getAidinkieli());
    hakemus.setAsiointikieli(toAtaruAsiointikieli(rivi.getAsiointikieli()));
    hakemus.setAsuinmaa(convertMaakoodi(rivi.getAsuinmaa(), koodistoCachedAsyncResource));
    hakemus.setEtunimi(rivi.getEtunimi());
    hakemus.setHenkilotunnus(rivi.getHenkilotunnus());
    hakemus.setKansalaisuus(convertMaakoodi(rivi.getKansalaisuus(), koodistoCachedAsyncResource));
    hakemus.setKotikunta(kotikunta);
    hakemus.setOsoite(rivi.getOsoite());
    hakemus.setPostinumero(rivi.getPostinumero());
    hakemus.setPostitoimipaikka(rivi.getPostitoimipaikka());
    hakemus.setPuhelinnumero(rivi.getPuhelinnumero());
    hakemus.setSukunimi(rivi.getSukunimi());
    hakemus.setSukupuoli(Sukupuoli.toHenkiloString(rivi.getSukupuoli()));
    hakemus.setSahkoposti(StringUtils.trimToEmpty(rivi.getSahkoposti()));
    hakemus.setSyntymaAika(rivi.getSyntymaAika());
    hakemus.setToisenAsteenSuoritus(rivi.getToisenAsteenSuoritus());
    hakemus.setToisenAsteenSuoritusmaa(
        convertMaakoodi(rivi.getToisenAsteenSuoritusmaa(), koodistoCachedAsyncResource));
    hakemus.setKutsumanimi(rivi.getKutsumanimi());
    hakemus.setSyntymapaikka(rivi.getSyntymapaikka());
    hakemus.setPassinNumero(rivi.getPassinNumero());
    hakemus.setIdTunnus(rivi.getIdTunnus());
    hakemus.setKaupunkiJaMaa(rivi.getKaupunkiJaMaa());

    return hakemus;
  }

  public static String kielisyysToString(KielisyysDto kielisyys) {
    if (kielisyys == null) {
      return "";
    } else {
      return kielisyys.getKieliKoodi();
    }
  }

  public static final Function<ErillishaunHakijaDTO, Boolean> ainoastaanHakemuksenTilaPaivitys =
      erillishakuRivi ->
          erillishakuRivi.getValintatuloksenTila() == null
              && erillishakuRivi.getIlmoittautumisTila() == null;

  public static ErillishaunHakijaDTO toErillishaunHakijaDTO(
      ErillishakuDTO haku, ErillishakuRivi rivi) {
    return new ErillishaunHakijaDTO(
        haku.getValintatapajonoOid(),
        rivi.getHakemusOid(),
        haku.getHakukohdeOid(),
        rivi.isJulkaistaankoTiedot(),
        rivi.getPersonOid(),
        haku.getHakuOid(),
        haku.getTarjoajaOid(),
        valintatuloksenTila(rivi),
        rivi.getEhdollisestiHyvaksyttavissa(),
        ilmoittautumisTila(rivi),
        hakemuksenTila(rivi),
        rivi.getEtunimi(),
        rivi.getSukunimi(),
        of(
            rivi.isPoistetaankoRivi()
                || StringUtils.isBlank(rivi.getHakemuksenTila())
                || isKesken(rivi)),
        rivi.getHyvaksymiskirjeLahetetty(),
        Lists.newArrayList(),
        rivi.getEhdollisenHyvaksymisenEhtoKoodi(),
        rivi.getEhdollisenHyvaksymisenEhtoFI(),
        rivi.getEhdollisenHyvaksymisenEhtoSV(),
        rivi.getEhdollisenHyvaksymisenEhtoEN());
  }

  public static ErillishaunHakijaDTO toPoistettavaErillishaunHakijaDTO(
      ErillishakuDTO haku, ErillishakuRivi rivi) {
    return new ErillishaunHakijaDTO(
        haku.getValintatapajonoOid(),
        rivi.getHakemusOid(),
        haku.getHakukohdeOid(),
        rivi.isJulkaistaankoTiedot(),
        rivi.getPersonOid(),
        haku.getHakuOid(),
        haku.getTarjoajaOid(),
        valintatuloksenTila(rivi),
        false,
        null,
        null,
        rivi.getEtunimi(),
        rivi.getSukunimi(),
        of(true),
        rivi.getHyvaksymiskirjeLahetetty(),
        Lists.newArrayList(),
        rivi.getEhdollisenHyvaksymisenEhtoKoodi(),
        rivi.getEhdollisenHyvaksymisenEhtoFI(),
        rivi.getEhdollisenHyvaksymisenEhtoSV(),
        rivi.getEhdollisenHyvaksymisenEhtoEN());
  }
}
