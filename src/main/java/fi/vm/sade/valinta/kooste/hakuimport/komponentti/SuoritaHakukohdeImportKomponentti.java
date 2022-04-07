package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import fi.vm.sade.service.valintaperusteet.dto.AvainArvoDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeImportDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdekoodiDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohteenValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.MonikielinenTekstiDTO;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.kouta.KoutaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.kouta.KoutaHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.kouta.KoutaValintakoe;
import fi.vm.sade.valinta.kooste.external.resource.kouta.PainotettuArvosana;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.*;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.HakukohdeValintaperusteetDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ValintakoeDTO;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
import fi.vm.sade.valinta.kooste.util.IterableUtil;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.camel.Body;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SuoritaHakukohdeImportKomponentti {
  private static final Logger LOG =
      LoggerFactory.getLogger(SuoritaHakukohdeImportKomponentti.class);
  private static final int KOUTA_HAKUKOHDE_OID_LENGTH = 35;
  private static final String NOLLA = "0.0";
  private static final String PAASYKOE_TYYPPI_URI = "valintakokeentyyppi_1";
  private static final String LISANAYTTO_TYYPPI_URI = "valintakokeentyyppi_2";
  private static final String PAINOKERROIN_POSTFIX = "_painokerroin";
  private static final String A11KIELI = "A1";
  private static final String A21KIELI = "A2";
  private static final String B21KIELI = "B2";
  private static final String B31KIELI = "B3";
  private static final String KOULUTUSTYYPPIKOODI_LUKIO = "koulutustyyppi_2";
  private static final String HAKUKOHDEKOODI_LUKIO = "hakukohteet_000";

  private TarjontaAsyncResource tarjontaAsyncResource;
  private KoutaAsyncResource koutaAsyncResource;
  private OrganisaatioAsyncResource organisaatioAsyncResource;
  private KoodistoCachedAsyncResource koodistoAsyncResource;

  @Autowired
  public SuoritaHakukohdeImportKomponentti(
      TarjontaAsyncResource tarjontaAsyncResource,
      KoutaAsyncResource koutaAsyncResource,
      OrganisaatioAsyncResource organisaatioAsyncResource,
      KoodistoCachedAsyncResource koodistoAsyncResource) {
    this.tarjontaAsyncResource = tarjontaAsyncResource;
    this.koutaAsyncResource = koutaAsyncResource;
    this.organisaatioAsyncResource = organisaatioAsyncResource;
    this.koodistoAsyncResource = koodistoAsyncResource;
  }

  private CompletableFuture<List<Pair<String, List<Koodi>>>> hakukohteetKooditYlakoodeineen(
      List<Koodi> koodit) {
    return CompletableFutureUtil.sequence(
        koodit.stream()
            .filter(koodi -> "hakukohteet".equals(koodi.getKoodistoUri()))
            .collect(
                Collectors.toMap(
                    Koodi::getKoodiUri,
                    Function.identity(),
                    (k, kk) -> k.getVersio() < kk.getVersio() ? kk : k))
            .values()
            .stream()
            .map(
                hakukohteetKoodi -> {
                  String uri = hakukohteetKoodi.getKoodiUri() + "#" + hakukohteetKoodi.getVersio();
                  return koodistoAsyncResource
                      .ylakoodit(uri)
                      .thenApplyAsync(ylakoodit -> Pair.of(uri, ylakoodit));
                })
            .collect(Collectors.toList()));
  }

  private boolean vastaavaPohjakoulutus(
      Pair<String, List<Koodi>> hakukohteet, String pohjakoulutusvaatimustoinenasteUri) {
    return hakukohteet.getRight().stream()
        .anyMatch(
            ylakoodi ->
                "pohjakoulutusvaatimustoinenaste".equals(ylakoodi.getKoodistoUri())
                    && pohjakoulutusvaatimustoinenasteUri.split("#")[0].equals(
                        ylakoodi.getKoodiUri()));
  }

  private boolean vainOsaamisalaanLiittyva(Pair<String, List<Koodi>> hakukohteet) {
    return hakukohteet.getRight().stream()
        .noneMatch(ylakoodi -> "koulutus".equals(ylakoodi.getKoodistoUri()));
  }

  public HakukohdeImportDTO suoritaHakukohdeImport(
      @Body // @Property(OPH.HAKUKOHDEOID)
          String hakukohdeOid) {
    try {
      if (hakukohdeOid.length() == KOUTA_HAKUKOHDE_OID_LENGTH) {
        return processKoutaHakukohde(hakukohdeOid);
      } else {
        return processTarjontaHakukohde(hakukohdeOid);
      }
    } catch (Exception e) {
      String msg = String.format("Importointi hakukohteelle %s epaonnistui!", hakukohdeOid);
      LOG.error(msg, e);
      throw new RuntimeException(msg, e);
    }
  }

  private HakukohdeImportDTO processCommonHakukohde(AbstractHakukohde hakukohde)
      throws InterruptedException, ExecutionException, TimeoutException {
    String hakukohdeKoodiTunniste = getHakukohdeKoodiTunniste(hakukohde);
    HakukohdeImportDTO importTyyppi = new HakukohdeImportDTO();
    Haku haku = tarjontaAsyncResource.haeHaku(hakukohde.hakuOid).get(5, TimeUnit.MINUTES);
    List<CompletableFuture<Toteutus>> toteutusFs =
        hakukohde.toteutusOids.stream()
            .map(tarjontaAsyncResource::haeToteutus)
            .collect(Collectors.toList());
    List<Koulutus> koulutukset =
        CompletableFutureUtil.sequence(
                toteutusFs.stream()
                    .map(
                        toteutusF ->
                            toteutusF.thenComposeAsync(
                                toteutus ->
                                    tarjontaAsyncResource.haeKoulutus(toteutus.koulutusOid)))
                    .collect(Collectors.toList()))
            .get(5, TimeUnit.MINUTES);
    List<Toteutus> toteutukset =
        CompletableFutureUtil.sequence(toteutusFs).get(5, TimeUnit.MINUTES);
    Map<String, Koodi> kaudet = koodistoAsyncResource.haeKoodisto("kausi");

    importTyyppi.setTarjoajaOid(hakukohde.tarjoajaOids.iterator().next());
    importTyyppi.setTarjoajaOids(hakukohde.tarjoajaOids);
    importTyyppi.setHaunkohdejoukkoUri(haku.kohdejoukkoUri);

    CompletableFutureUtil.sequence(
            hakukohde.tarjoajaOids.stream()
                .map(organisaatioAsyncResource::haeOrganisaatio)
                .collect(Collectors.toList()))
        .get(5, TimeUnit.MINUTES)
        .forEach(
            tarjoaja ->
                tarjoaja
                    .getNimi()
                    .forEach(
                        (kieli, nimi) -> {
                          MonikielinenTekstiDTO dto = new MonikielinenTekstiDTO();
                          dto.setLang("kieli_" + kieli);
                          dto.setText(nimi);
                          importTyyppi.getTarjoajaNimi().add(dto);
                        }));

    hakukohde.nimi.forEach(
        (kieli, nimi) -> {
          MonikielinenTekstiDTO dto = new MonikielinenTekstiDTO();
          dto.setLang(kieli);
          dto.setText(nimi);
          importTyyppi.getHakukohdeNimi().add(dto);
        });

    if (haku.hakukausiUri != null) {
      kaudet.forEach(
          (arvo, koodi) -> {
            if (haku.hakukausiUri.startsWith(koodi.getKoodiUri())) {
              koodi
                  .getMetadata()
                  .forEach(
                      metadata -> {
                        MonikielinenTekstiDTO dto = new MonikielinenTekstiDTO();
                        dto.setLang("kieli_" + metadata.getKieli().toLowerCase());
                        dto.setText(metadata.getNimi());
                        importTyyppi.getHakuKausi().add(dto);
                      });
            }
          });
    }

    if (haku.hakukausiVuosi != null) {
      importTyyppi.setHakuVuosi(Integer.toString(haku.hakukausiVuosi));
    }

    HakukohdekoodiDTO hkt = new HakukohdekoodiDTO();
    String hakukohteetUri = hakukohde.hakukohteetUri;
    String pohjakoulutusvaatimustoinenasteUri =
        IterableUtil.singleton(
            hakukohde.pohjakoulutusvaatimusUrit.stream()
                    .filter(uri -> uri.startsWith("pohjakoulutusvaatimustoinenaste_"))
                ::iterator);
    if (hakukohteetUri == null && pohjakoulutusvaatimustoinenasteUri != null) {
      String osaamisalaUri =
          IterableUtil.singleton(
              toteutukset.stream()
                      .flatMap(toteutus -> toteutus.osaamisalaUris.stream())
                      .filter(Objects::nonNull)
                      .distinct()
                  ::iterator);
      if (osaamisalaUri != null) {
        hakukohteetUri =
            koodistoAsyncResource
                .alakoodit(osaamisalaUri)
                .thenComposeAsync(this::hakukohteetKooditYlakoodeineen)
                .thenApplyAsync(
                    hakukohteetKoodit ->
                        IterableUtil.singleton(
                            hakukohteetKoodit.stream()
                                    .filter(
                                        h ->
                                            this.vastaavaPohjakoulutus(
                                                    h, pohjakoulutusvaatimustoinenasteUri)
                                                && this.vainOsaamisalaanLiittyva(h))
                                    .map(Pair::getLeft)
                                ::iterator))
                .get(5, TimeUnit.MINUTES);
      }
      if (hakukohteetUri == null) {
        String koulutusUri =
            IterableUtil.singleton(
                koulutukset.stream()
                        .map(koulutus -> koulutus.koulutusUrit)
                        .flatMap(koulutusUrit -> koulutusUrit.stream())
                        .filter(Objects::nonNull)
                        .distinct()
                    ::iterator);
        if (koulutusUri != null) {
          hakukohteetUri =
              koodistoAsyncResource
                  .alakoodit(koulutusUri)
                  .thenComposeAsync(this::hakukohteetKooditYlakoodeineen)
                  .thenApplyAsync(
                      hakukohteetKoodit ->
                          IterableUtil.singleton(
                              hakukohteetKoodit.stream()
                                      .filter(
                                          h ->
                                              this.vastaavaPohjakoulutus(
                                                  h, pohjakoulutusvaatimustoinenasteUri))
                                      .map(Pair::getLeft)
                                  ::iterator))
                  .get(5, TimeUnit.MINUTES);
        }
      }
    }
    if (hakukohteetUri == null) {
      hakukohteetUri = "hakukohteet_" + hakukohde.oid.replace(".", "");
    }
    hkt.setKoodiUri(hakukohteetUri);
    importTyyppi.setHakukohdekoodi(hkt);

    importTyyppi.setHakukohdeOid(hakukohde.oid);
    importTyyppi.setHakuOid(hakukohde.hakuOid);
    importTyyppi.setTila(hakukohde.tila.name());
    importTyyppi.setValinnanAloituspaikat(
        Objects.requireNonNullElse(hakukohde.valintojenAloituspaikat, 0));

    AvainArvoDTO avainArvo = new AvainArvoDTO();

    avainArvo.setAvain("hakukohde_oid");
    avainArvo.setArvo(hakukohde.oid);
    importTyyppi.getValintaperuste().add(avainArvo);

    avainArvo = new AvainArvoDTO();
    avainArvo.setAvain("riittava_kielitaito_tunniste");
    avainArvo.setArvo(hakukohdeKoodiTunniste + "_riittava_kielitaito");
    importTyyppi.getValintaperuste().add(avainArvo);

    String opetuskieli =
        toteutukset.stream()
            .flatMap(koulutus -> koulutus.opetuskielet.stream())
            .map(uri -> uri.replace("kieli_", ""))
            .findAny()
            .orElse(null);
    avainArvo = new AvainArvoDTO();
    avainArvo.setAvain("opetuskieli");
    avainArvo.setArvo(opetuskieli);
    importTyyppi.getValintaperuste().add(avainArvo);

    // Kielikoetunnisteen selvittäminen
    String kielikoetunniste;
    if (StringUtils.isNotBlank(opetuskieli)) {
      kielikoetunniste = "kielikoe_" + opetuskieli;
    } else {
      kielikoetunniste = hakukohdeKoodiTunniste + "_kielikoe";
    }

    avainArvo = new AvainArvoDTO();
    avainArvo.setAvain("kielikoe_tunniste");
    avainArvo.setArvo(kielikoetunniste);
    importTyyppi.getValintaperuste().add(avainArvo);
    return importTyyppi;
  }

  private HakukohdeImportDTO processKoutaHakukohde(String hakukohdeOid)
      throws ExecutionException, InterruptedException, TimeoutException {
    KoutaHakukohde hakukohde =
        this.koutaAsyncResource.haeHakukohde(hakukohdeOid).get(5, TimeUnit.MINUTES);
    HakukohdeImportDTO importTyyppi = processCommonHakukohde(hakukohde);

    HakukohdekoodiDTO hakukohdekoodi = new HakukohdekoodiDTO();
    if (hakukohde.koulutustyyppikoodi != null
        && hakukohde.koulutustyyppikoodi.equals(KOULUTUSTYYPPIKOODI_LUKIO)) {
      hakukohdekoodi.setKoodiUri(HAKUKOHDEKOODI_LUKIO);
      importTyyppi.setHakukohdekoodi(hakukohdekoodi);
    } else if (hakukohde.hakukohdeKoodiUri != null) {
      hakukohdekoodi.setKoodiUri(hakukohde.hakukohdeKoodiUri);
      importTyyppi.setHakukohdekoodi(hakukohdekoodi);
    }

    List<HakukohteenValintakoeDTO> uniqueValintakokeet =
        Set.of(PAASYKOE_TYYPPI_URI, LISANAYTTO_TYYPPI_URI ).stream()
            .map(hakukohde::getValintakoeOfType)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(
                vk -> {
                  HakukohteenValintakoeDTO dto = new HakukohteenValintakoeDTO();
                  dto.setOid(vk.id);
                  dto.setTyyppiUri(vk.valintakokeentyyppiUri);
                  return dto;
                })
            .collect(Collectors.toList());
    importTyyppi.setValintakoe(uniqueValintakokeet);

    addAvainArvoToValintaperuste(
        importTyyppi,
        "painotettu_keskiarvo_hylkays_max",
        hakukohde.alinHyvaksyttyKeskiarvo != null
            ? hakukohde.alinHyvaksyttyKeskiarvo.toString()
            : NOLLA);

    Optional<KoutaValintakoe> paasykoe = hakukohde.getValintakoeOfType(PAASYKOE_TYYPPI_URI);
    paasykoe.map(pk -> pk.vahimmaispisteet).ifPresent(
        pisteet ->
            addAvainArvoToValintaperuste(
                importTyyppi, "paasykoe_hylkays_max", pisteet.toString()));

    Optional<KoutaValintakoe> lisanaytto = hakukohde.getValintakoeOfType(LISANAYTTO_TYYPPI_URI);
    lisanaytto.map(pk -> pk.vahimmaispisteet).ifPresent(
        pisteet ->
            addAvainArvoToValintaperuste(
                importTyyppi, "lisanaytto_hylkays_max", pisteet.toString()));

    Map<String, Koodi> koodiarvoKoodi =
        koodistoAsyncResource.haeKoodisto(
            KoodistoCachedAsyncResource.PAINOTETTAVAT_OPPIAINEET_LUKIOSSA);
    Map<String, String> koodiUriKoodiArvo = new HashMap<>();
    for (Koodi koodi : koodiarvoKoodi.values()) {
      koodiUriKoodiArvo.put(koodi.getKoodiUri(), koodi.getKoodiArvo());
    }

    for (PainotettuArvosana arvosana : hakukohde.painotetutArvosanat) {
      String koodiarvo = koodiUriKoodiArvo.get(arvosana.koodiUri);
      if (koodiarvo != null && !koodiarvo.isEmpty()){
        addAvainArvoToValintaperuste(
                importTyyppi, koodiarvo + PAINOKERROIN_POSTFIX, arvosana.painokerroin.toString());

        String oppiaine = koodiarvo.split("_")[0];
        if (oppiaine.equals(A11KIELI)
                || oppiaine.equals(A21KIELI)
                || oppiaine.equals(B21KIELI)
                || oppiaine.equals(B31KIELI)) {
          // koodiarvo is formatted A1_FI
          String kieli = koodiarvo.split("_")[1];

          String toinenKieli = oppiaine + "2_" + kieli + PAINOKERROIN_POSTFIX;
          addAvainArvoToValintaperuste(importTyyppi, toinenKieli, arvosana.painokerroin.toString());
          String kolmasKieli = oppiaine + "3_" + kieli + PAINOKERROIN_POSTFIX;
          addAvainArvoToValintaperuste(importTyyppi, kolmasKieli, arvosana.painokerroin.toString());
        }
      }
    }

    return importTyyppi;
  }

  private HakukohdeImportDTO processTarjontaHakukohde(String hakukohdeOid)
      throws InterruptedException, ExecutionException, TimeoutException {
    AbstractHakukohde hakukohde =
        this.tarjontaAsyncResource.haeHakukohde(hakukohdeOid).get(5, TimeUnit.MINUTES);
    String hakukohdeKoodiTunniste = getHakukohdeKoodiTunniste(hakukohde);
    HakukohdeImportDTO importTyyppi = processCommonHakukohde(hakukohde);

    AvainArvoDTO avainArvo;

    HakukohdeValintaperusteetDTO valintaperusteet =
        tarjontaAsyncResource.findValintaperusteetByOid(hakukohdeOid).get(60, TimeUnit.SECONDS);

    importTyyppi.setValintakoe(new ArrayList<>());
    for (ValintakoeDTO valintakoeDTO : valintaperusteet.getValintakokeet()) {
      HakukohteenValintakoeDTO v = new HakukohteenValintakoeDTO();
      v.setOid(valintakoeDTO.getOid());
      v.setTyyppiUri(valintakoeDTO.getTyyppiUri());
      importTyyppi.getValintakoe().add(v);
    }

    addAvainArvoToValintaperuste(
        importTyyppi, "paasykoe_min", valintaperusteet.getPaasykoeMin().toString());
    addAvainArvoToValintaperuste(
        importTyyppi, "paasykoe_max", valintaperusteet.getPaasykoeMax().toString());
    addAvainArvoToValintaperuste(
        importTyyppi, "paasykoe_hylkays_min", valintaperusteet.getPaasykoeHylkaysMin().toString());
    addAvainArvoToValintaperuste(
        importTyyppi, "paasykoe_hylkays_max", valintaperusteet.getPaasykoeHylkaysMax().toString());
    addAvainArvoToValintaperuste(
        importTyyppi, "lisanaytto_min", valintaperusteet.getLisanayttoMin().toString());
    addAvainArvoToValintaperuste(
        importTyyppi, "lisanaytto_max", valintaperusteet.getLisanayttoMax().toString());
    addAvainArvoToValintaperuste(
        importTyyppi,
        "lisanaytto_hylkays_min",
        valintaperusteet.getLisanayttoHylkaysMin().toString());
    addAvainArvoToValintaperuste(
        importTyyppi,
        "lisanaytto_hylkays_max",
        valintaperusteet.getLisanayttoHylkaysMax().toString());
    addAvainArvoToValintaperuste(
        importTyyppi,
        "paasykoe_ja_lisanaytto_hylkays_min",
        valintaperusteet.getHylkaysMin().toString());
    addAvainArvoToValintaperuste(
        importTyyppi,
        "paasykoe_ja_lisanaytto_hylkays_max",
        valintaperusteet.getHylkaysMax().toString());
    addAvainArvoToValintaperuste(
        importTyyppi,
        "painotettu_keskiarvo_hylkays_min",
        valintaperusteet.getPainotettuKeskiarvoHylkaysMin().toString());
    addAvainArvoToValintaperuste(
        importTyyppi,
        "painotettu_keskiarvo_hylkays_max",
        valintaperusteet.getPainotettuKeskiarvoHylkaysMax().toString());
    addAvainArvoToValintaperuste(
        importTyyppi,
        "paasykoe_tunniste",
        valintaperusteet.getPaasykoeTunniste() != null
            ? valintaperusteet.getPaasykoeTunniste()
            : hakukohdeKoodiTunniste + "_paasykoe");
    addAvainArvoToValintaperuste(
        importTyyppi,
        "lisanaytto_tunniste",
        valintaperusteet.getLisanayttoTunniste() != null
            ? valintaperusteet.getLisanayttoTunniste()
            : hakukohdeKoodiTunniste + "_lisanaytto");
    addAvainArvoToValintaperuste(
        importTyyppi,
        "lisapiste_tunniste",
        valintaperusteet.getLisapisteTunniste() != null
            ? valintaperusteet.getLisapisteTunniste()
            : hakukohdeKoodiTunniste + "_lisapiste");
    addAvainArvoToValintaperuste(
        importTyyppi,
        "urheilija_lisapiste_tunniste",
        valintaperusteet.getUrheilijaLisapisteTunniste() != null
            ? valintaperusteet.getUrheilijaLisapisteTunniste()
            : hakukohdeKoodiTunniste + "_urheilija_lisapiste");

    for (String avain : valintaperusteet.getPainokertoimet().keySet()) {
      addAvainArvoToValintaperuste(
          importTyyppi, avain, valintaperusteet.getPainokertoimet().get(avain));
    }

    return importTyyppi;
  }

  private String getHakukohdeKoodiTunniste(AbstractHakukohde hakukohde) {
    return hakukohde.oid.replace(".", "_");
  }

  private void addAvainArvoToValintaperuste(
      HakukohdeImportDTO importTyyppi, String avain, String arvo) {
    AvainArvoDTO avainArvo = new AvainArvoDTO();
    avainArvo.setAvain(avain);
    avainArvo.setArvo(arvo);
    importTyyppi.getValintaperuste().add(avainArvo);
  }
}
