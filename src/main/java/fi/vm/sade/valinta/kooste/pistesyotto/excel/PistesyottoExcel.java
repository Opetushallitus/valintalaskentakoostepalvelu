package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import static fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO.*;
import static java.util.Comparator.comparing;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.model.Funktiotyyppi;
import fi.vm.sade.valinta.kooste.excel.*;
import fi.vm.sade.valinta.kooste.excel.arvo.*;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.util.ApplicationAdditionalDataComparator;
import fi.vm.sade.valinta.kooste.util.Formatter;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.KonversioBuilder;
import fi.vm.sade.valinta.kooste.valintalaskenta.tulos.predicate.OsallistujatPredicate;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.*;
import fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PistesyottoExcel {
  private static final Logger LOG = LoggerFactory.getLogger(PistesyottoExcel.class);

  private static final String MERKITSEMATTA = "Merkitsemättä";
  private static final String OSALLISTUI = "Osallistui";
  private static final String EI_OSALLISTUNUT = "Ei osallistunut";
  private static final String EI_VAADITA = "Ei vaadita";

  public static final String VAKIO_MERKITSEMATTA = "MERKITSEMATTA";
  public static final String VAKIO_OSALLISTUI = "OSALLISTUI";
  public static final String VAKIO_EI_OSALLISTUNUT = "EI_OSALLISTUNUT";
  public static final String VAKIO_EI_VAADITA = "EI_VAADITA";
  private AikaleimaRivi aikaleimaRivi = new AikaleimaRivi();
  private static final Collection<String> VAIHTOEHDOT =
      Arrays.asList(MERKITSEMATTA, OSALLISTUI, EI_OSALLISTUNUT, EI_VAADITA);
  private static final Map<String, String> VAIHTOEHDOT_KONVERSIO =
      new KonversioBuilder()
          .addKonversio(StringUtils.EMPTY, MERKITSEMATTA)
          .addKonversio(VAKIO_MERKITSEMATTA, MERKITSEMATTA)
          .addKonversio(VAKIO_OSALLISTUI, OSALLISTUI)
          .addKonversio(VAKIO_EI_VAADITA, EI_VAADITA)
          .addKonversio(VAKIO_EI_OSALLISTUNUT, EI_OSALLISTUNUT)
          .build();
  private static final Map<String, String> VAIHTOEHDOT_TAKAISINPAIN_KONVERSIO =
      new KonversioBuilder()
          .addKonversio(StringUtils.EMPTY, VAKIO_MERKITSEMATTA)
          .addKonversio(MERKITSEMATTA, VAKIO_MERKITSEMATTA)
          .addKonversio(OSALLISTUI, VAKIO_OSALLISTUI)
          .addKonversio(EI_VAADITA, VAKIO_EI_VAADITA)
          .addKonversio(EI_OSALLISTUNUT, VAKIO_EI_OSALLISTUNUT)
          .build();

  private static final String KIELIKOE_TUNNISTE = "kielikoe";
  public static final String TYHJA = "Tyhjä";

  public static final String HYVAKSYTTY = "Hyväksytty";
  public static final String HYLATTY = "Hylätty";
  private static final Collection<String> TOTUUSARVO_KIELIKOE =
      Arrays.asList(TYHJA, HYVAKSYTTY, HYLATTY);
  private static final Map<String, String> TOTUUSARVO_KIELIKOE_KONVERSIO =
      new KonversioBuilder()
          .addKonversio(HYVAKSYTTY, Boolean.TRUE.toString())
          .addKonversio(HYLATTY, Boolean.FALSE.toString())
          .addKonversio(TYHJA, null)
          .build();

  public static final String KYLLA = "Kyllä";
  public static final String EI = "Ei";

  public static final String KIELIKOE_REGEX = "kielikoe\\_\\p{Lower}\\p{Lower}";

  private static final Collection<String> TOTUUSARVO = Arrays.asList(TYHJA, KYLLA, EI);
  private static final Map<String, String> TOTUUSARVO_KONVERSIO =
      new KonversioBuilder()
          .addKonversio(KYLLA, Boolean.TRUE.toString())
          .addKonversio(EI, Boolean.FALSE.toString())
          .addKonversio(TYHJA, null)
          .build();

  private final Excel excel;
  private Map<String, Boolean> onkoHakijaOsallistuja = Maps.newHashMap();

  private String hakemusValintakoeYhdiste(String hakemus, String valintakoe) {
    return new StringBuilder().append(hakemus).append("_").append(valintakoe).toString();
  }

  public boolean onkoHakijaOsallistujaValintakokeeseen(String hakemus, String valintakoe) {
    return onkoHakijaOsallistuja.getOrDefault(hakemusValintakoeYhdiste(hakemus, valintakoe), false);
  }

  /**
   * @return < HakemusOid , < Tunniste , ValintakoeDTO > >
   */
  private Map<String, Map<String, ValintakoeDTO>> valintakoeOidit(
      String hakukohdeOid, List<ValintakoeOsallistuminenDTO> osallistumistiedot) {
    Map<String, Map<String, ValintakoeDTO>> tunnisteOid = Maps.newHashMap();
    for (ValintakoeOsallistuminenDTO o : osallistumistiedot) {
      for (HakutoiveDTO h : o.getHakutoiveet()) {
        if (!hakukohdeOid.equals(h.getHakukohdeOid())) {
          continue;
        }
        Map<String, ValintakoeDTO> k = Maps.newHashMap();
        for (ValintakoeValinnanvaiheDTO v : h.getValinnanVaiheet()) {
          for (ValintakoeDTO valintakoe : v.getValintakokeet()) {
            k.put(valintakoe.getValintakoeTunniste(), valintakoe);
          }
        }
        tunnisteOid.put(o.getHakemusOid(), k);
      }
    }
    return tunnisteOid;
  }

  public Optional<String> getAikaleima() {
    return aikaleimaRivi.getCurrentAikaleima();
  }

  public PistesyottoExcel(
      String hakuOid,
      String hakukohdeOid,
      String tarjoajaOid,
      String hakuNimi,
      String hakukohdeNimi,
      String tarjoajaNimi,
      Optional<String> aikaleima,
      Collection<HakemusWrapper> hakemukset,
      Set<String> kaikkiKutsutaanTunnisteet,
      Collection<String> valintakoeTunnisteet,
      List<ValintakoeOsallistuminenDTO> osallistumistiedot,
      List<ValintaperusteDTO> valintaperusteet,
      List<ApplicationAdditionalDataDTO> kaikkiPistetiedot,
      Collection<PistesyottoDataRiviKuuntelija> kuuntelijat) {
    this.aikaleimaRivi = new AikaleimaRivi(aikaleima);
    if (kaikkiPistetiedot == null) {
      kaikkiPistetiedot = Collections.emptyList();
    }

    Collections.sort(valintaperusteet, comparing(ValintaperusteDTO::getKuvaus));

    Map<String, Map<String, ValintakoeDTO>> tunnisteValintakoe =
        valintakoeOidit(hakukohdeOid, osallistumistiedot);

    final Set<String> osallistujat =
        osallistumistiedot.stream()
            .filter(
                o ->
                    OsallistujatPredicate.vainOsallistujatTunnisteella(
                            hakukohdeOid, valintakoeTunnisteet)
                        .apply(o))
            .map(ValintakoeOsallistuminenDTO::getHakemusOid)
            .collect(Collectors.toSet());

    final List<String> tunnisteet =
        valintaperusteet.stream().map(ValintaperusteDTO::getTunniste).collect(Collectors.toList());

    List<Rivi> rivit = Lists.newArrayList();
    rivit.add(aikaleimaRivi);
    rivit.add(new RiviBuilder().addOid(hakuOid).addTeksti(hakuNimi, 4).build());
    rivit.add(new RiviBuilder().addOid(hakukohdeOid).addTeksti(hakukohdeNimi, 4).build());
    if (StringUtils.isBlank(tarjoajaOid)) {
      rivit.add(Rivi.tyhjaRivi());
    } else {
      rivit.add(new RiviBuilder().addOid(tarjoajaOid).addTeksti(tarjoajaNimi, 4).build());
    }
    rivit.add(Rivi.tyhjaRivi());
    rivit.add(
        new RiviBuilder()
            .addTyhja()
            .addTyhja()
            .addTyhja()
            .addTyhja()
            .addRivi(new OidRivi(tunnisteet, 2, true))
            .build());
    final RiviBuilder valintakoeOtsikkoRiviBuilder = new RiviBuilder();
    final RiviBuilder otsikkoRiviBuilder =
        new RiviBuilder()
            .addKeskitettyTeksti("Hakemus OID")
            .addKeskitettyTeksti("Tiedot")
            .addKeskitettyTeksti("Henkilötunnus")
            .addKeskitettyTeksti("Syntymäaika");
    valintakoeOtsikkoRiviBuilder.addTyhja().addTyhja().addTyhja().addTyhja();

    for (String valintakoe : createValintakokeet(valintaperusteet)) {
      otsikkoRiviBuilder.addTyhja().addKeskitettyTeksti("Osallistuminen");
      valintakoeOtsikkoRiviBuilder.addSolu(new Teksti(valintakoe, true, true, false, 0, 2, false));
    }
    rivit.add(valintakoeOtsikkoRiviBuilder.build());
    rivit.add(otsikkoRiviBuilder.build());

    Collection<Collection<Arvo>> sx = Lists.newArrayList();

    Collections.sort(kaikkiPistetiedot, ApplicationAdditionalDataComparator.ASCENDING);

    // Asennetaan konvertterit
    Collection<PistesyottoDataArvo> dataArvot = getPistesyotonDataArvot(valintaperusteet);
    Predicate<ValintakoeDTO> osallistuuValintakokeeseen =
        valintakoe ->
            (valintakoe != null
                && Osallistuminen.OSALLISTUU.equals(
                    Optional.ofNullable(valintakoe.getOsallistuminenTulos())
                        .orElse(new OsallistuminenTulosDTO())
                        .getOsallistuminen()));
    Map<String, HakemusWrapper> oidToWrapper =
        hakemukset.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h));
    List<ApplicationAdditionalDataDTO> pistetiedotHakuAppistaLoytyvilleHakemuksille =
        filteroiPistetiedoistaPoisNeJoilleEiLoydyAktiivistaHakemustaHakuAppista(
                hakemukset, kaikkiPistetiedot)
            .sorted(
                (a0, a1) -> {
                  HakemusWrapper w0 = oidToWrapper.get(a0.getOid());
                  HakemusWrapper w1 = oidToWrapper.get(a1.getOid());
                  int c = w0.getSukunimi().compareTo(w1.getSukunimi());
                  return c == 0 ? w0.getEtunimet().compareTo(w1.getEtunimet()) : c;
                })
            .collect(Collectors.toList());
    for (ApplicationAdditionalDataDTO data : pistetiedotHakuAppistaLoytyvilleHakemuksille) {
      final String hakemusOid = data.getOid();
      final boolean mahdollinenOsallistuja = osallistujat.contains(hakemusOid);
      // Hakemuksen <tunniste, valintakoeDTO> tiedot
      Map<String, ValintakoeDTO> tunnisteDTO =
          Optional.ofNullable(tunnisteValintakoe.get(data.getOid())).orElse(Collections.emptyMap());
      Collection<Arvo> s = Lists.newArrayList();
      HakemusWrapper wrapper = oidToWrapper.get(data.getOid());
      s.add(new TekstiArvo(data.getOid()));
      s.add(new TekstiArvo(wrapper.getSukunimi() + ", " + wrapper.getEtunimet()));
      s.add(new TekstiArvo(null == wrapper.getHenkilotunnus() ? "" : wrapper.getHenkilotunnus()));
      s.add(new TekstiArvo(null == wrapper.getSyntymaaika() ? "" : wrapper.getSyntymaaika()));
      boolean syote = false;
      for (ValintaperusteDTO valintaperuste : valintaperusteet) {
        ValintakoeDTO valintakoe =
            Optional.ofNullable(tunnisteDTO.get(valintaperuste.getTunniste()))
                .orElse(new ValintakoeDTO());
        final boolean syotettavissaKaikille =
            kaikkiKutsutaanTunnisteet.contains(valintaperuste.getTunniste())
                || Boolean.TRUE.equals(valintaperuste.getSyotettavissaKaikille());
        if (syotettavissaKaikille
            || (mahdollinenOsallistuja && osallistuuValintakokeeseen.test(valintakoe))) {
          onkoHakijaOsallistuja.put(
              hakemusValintakoeYhdiste(hakemusOid, valintaperuste.getTunniste()), true);
          syote = true;
          if (Funktiotyyppi.LUKUARVOFUNKTIO.equals(valintaperuste.getFunktiotyyppi())) {
            if (valintaperuste.getArvot() != null && !valintaperuste.getArvot().isEmpty()) {
              String value = null;
              if (data.getAdditionalData() != null) {
                value = data.getAdditionalData().get(valintaperuste.getTunniste());
              }
              s.add(new MonivalintaArvo(value, valintaperuste.getArvot()));
            } else {
              Number value = null;
              if (data.getAdditionalData() != null) {
                value = asNumber(data.getAdditionalData().get(valintaperuste.getTunniste()));
              }
              Number max = asNumber(valintaperuste.getMax());
              Number min = asNumber(valintaperuste.getMin());
              s.add(new NumeroArvo(value, min, max));
            }
          } else if (Funktiotyyppi.TOTUUSARVOFUNKTIO.equals(valintaperuste.getFunktiotyyppi())) {
            String value =
                StringUtils.trimToEmpty(data.getAdditionalData().get(valintaperuste.getTunniste()));
            if (isKielikoe(valintaperuste)) {
              s.add(new BooleanArvo(value, TOTUUSARVO_KIELIKOE, HYVAKSYTTY, HYLATTY, TYHJA));
            } else {
              s.add(new BooleanArvo(value, TOTUUSARVO, KYLLA, EI, TYHJA));
            }
          } else {
            s.add(
                new TekstiArvo(
                    data.getAdditionalData()
                        .get(StringUtils.trimToEmpty(valintaperuste.getTunniste())),
                    false));
          }
          s.add(
              new MonivalintaArvo(
                  VAIHTOEHDOT_KONVERSIO.get(
                      StringUtils.trimToEmpty(
                          data.getAdditionalData()
                              .get(valintaperuste.getOsallistuminenTunniste()))),
                  VAIHTOEHDOT));
        } else { // Ei ole kaikille syotettava arvo eika hakija ole tahan valintakokeeseen
          // osallistuja
          s.add(TekstiArvo.editoimatonTyhja());
          s.add(TekstiArvo.editoimatonTyhja());
        }
      }
      if (syote) {
        sx.add(s);
      }
    }
    if (sx.isEmpty()) {
      throw new RuntimeException("Hakukohteessa ei ole pistesyötettäviä hakijoita.");
    }
    rivit.add(new PistesyottoDataRivi(sx, kuuntelijat, dataArvot));
    // Piilotettavat sarakkeet:
    // Piilotettavat rivit: 4=valintakoetunnisteet
    this.excel = new Excel("Pistesyöttö", rivit, new int[] {}, new int[] {4});
  }

  private Stream<ApplicationAdditionalDataDTO>
      filteroiPistetiedoistaPoisNeJoilleEiLoydyAktiivistaHakemustaHakuAppista(
          Collection<HakemusWrapper> hakemukset, List<ApplicationAdditionalDataDTO> pistetiedot) {
    Set<String> hakuAppistaLoytyvatHakemusOidit =
        hakemukset.stream().map(HakemusWrapper::getOid).collect(Collectors.toSet());
    return pistetiedot.stream()
        .filter(
            a -> {
              boolean loytyyHakuAppista = hakuAppistaLoytyvatHakemusOidit.contains(a.getOid());
              if (!loytyyHakuAppista) {
                LOG.warn(
                    String.format(
                        "Hakemuksen %s pistetiedot jätetään huomioimatta, "
                            + "koska sille ei ole löytynyt aktiivista hakemusta.",
                        a.getOid()));
              }
              return loytyyHakuAppista;
            });
  }

  private Collection<PistesyottoDataArvo> getPistesyotonDataArvot(
      List<ValintaperusteDTO> valintaperusteet) {
    Collection<PistesyottoDataArvo> dataArvot = Lists.newArrayList();
    for (ValintaperusteDTO valintaperuste : valintaperusteet) {
      LOG.info(
          "Tunniste=={}, osallistumisentunniste={}",
          valintaperuste.getTunniste(),
          valintaperuste.getOsallistuminenTunniste());
      if (Funktiotyyppi.LUKUARVOFUNKTIO.equals(valintaperuste.getFunktiotyyppi())) {
        Double max = asNumber(valintaperuste.getMax());
        Double min = asNumber(valintaperuste.getMin());
        if (min != null && max != null) {
          dataArvot.add(
              new NumeroDataArvo(
                  min,
                  max,
                  VAIHTOEHDOT_TAKAISINPAIN_KONVERSIO,
                  StringUtils.trimToEmpty(valintaperuste.getTunniste()),
                  VAKIO_OSALLISTUI,
                  StringUtils.trimToEmpty(valintaperuste.getOsallistuminenTunniste())));
        } else if (valintaperuste.getArvot() != null && !valintaperuste.getArvot().isEmpty()) {
          dataArvot.add(
              new DiskreettiDataArvo(
                  valintaperuste.getArvot(),
                  VAIHTOEHDOT_TAKAISINPAIN_KONVERSIO,
                  StringUtils.trimToEmpty(valintaperuste.getTunniste()),
                  VAKIO_OSALLISTUI,
                  StringUtils.trimToEmpty(valintaperuste.getOsallistuminenTunniste())));
        } else {
          dataArvot.add(
              new NumeroDataArvo(
                  0,
                  0,
                  VAIHTOEHDOT_TAKAISINPAIN_KONVERSIO,
                  StringUtils.trimToEmpty(valintaperuste.getTunniste()),
                  VAKIO_OSALLISTUI,
                  StringUtils.trimToEmpty(valintaperuste.getOsallistuminenTunniste())));
        }
      } else if (Funktiotyyppi.TOTUUSARVOFUNKTIO.equals(valintaperuste.getFunktiotyyppi())) {
        if (isKielikoe(valintaperuste)) {
          dataArvot.add(
              new BooleanDataArvo(
                  TOTUUSARVO_KIELIKOE_KONVERSIO,
                  VAIHTOEHDOT_TAKAISINPAIN_KONVERSIO,
                  StringUtils.trimToEmpty(valintaperuste.getTunniste()),
                  VAKIO_OSALLISTUI,
                  StringUtils.trimToEmpty(valintaperuste.getOsallistuminenTunniste())));
        } else {
          dataArvot.add(
              new BooleanDataArvo(
                  TOTUUSARVO_KONVERSIO,
                  VAIHTOEHDOT_TAKAISINPAIN_KONVERSIO,
                  StringUtils.trimToEmpty(valintaperuste.getTunniste()),
                  VAKIO_OSALLISTUI,
                  StringUtils.trimToEmpty(valintaperuste.getOsallistuminenTunniste())));
        }
      } else {
        LOG.error("Tunnistamaton funktiotyyppi! Peruutetaan pistesyoton luonti!");
        throw new RuntimeException("Tunnistamaton syote! Peruutetaan pistesyoton luonti!");
      }
    }
    return dataArvot;
  }

  private List<String> createValintakokeet(List<ValintaperusteDTO> valintaperusteet) {
    return valintaperusteet.stream()
        .map(
            valintaperuste -> {
              if (Funktiotyyppi.LUKUARVOFUNKTIO.equals(valintaperuste.getFunktiotyyppi())
                  && !StringUtils.isBlank(valintaperuste.getMin())
                  && !StringUtils.isBlank(valintaperuste.getMax())) {
                // create value constraint
                return valintaperuste.getKuvaus()
                    + " ("
                    + Formatter.suomennaNumero(new BigDecimal(valintaperuste.getMin()))
                    + " - "
                    + Formatter.suomennaNumero(new BigDecimal(valintaperuste.getMax()))
                    + ")";
              } else {
                return valintaperuste.getKuvaus();
              }
            })
        .collect(Collectors.toList());
  }

  private Double asNumber(String value) {
    if (StringUtils.isBlank(value)) {
      return null;
    } else {
      value = StringUtils.trimToEmpty(value).replace(",", ".");
      try {
        return Double.parseDouble(value);
      } catch (Exception e) {
        return null;
      }
    }
  }

  private boolean isKielikoe(ValintaperusteDTO valintaperuste) {
    if (valintaperuste == null || valintaperuste.getTunniste() == null) return false;
    return valintaperuste.getTunniste().contains(KIELIKOE_TUNNISTE);
  }

  public Excel getExcel() {
    return excel;
  }
}
