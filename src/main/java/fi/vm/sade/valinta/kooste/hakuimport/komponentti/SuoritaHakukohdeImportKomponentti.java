package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import fi.vm.sade.service.valintaperusteet.dto.AvainArvoDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeImportDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdekoodiDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohteenValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.MonikielinenTekstiDTO;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Hakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Koulutus;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Toteutus;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Valintakoe;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.HakukohdeValintaperusteetDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ValintakoeDTO;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
import fi.vm.sade.valinta.kooste.util.IterableUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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

  private TarjontaAsyncResource tarjontaAsyncResource;
  private OrganisaatioAsyncResource organisaatioAsyncResource;
  private KoodistoCachedAsyncResource koodistoAsyncResource;

  @Autowired
  public SuoritaHakukohdeImportKomponentti(
      TarjontaAsyncResource tarjontaAsyncResource,
      OrganisaatioAsyncResource organisaatioAsyncResource,
      KoodistoCachedAsyncResource koodistoAsyncResource) {
    this.tarjontaAsyncResource = tarjontaAsyncResource;
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
      Hakukohde hakukohde =
          tarjontaAsyncResource.haeHakukohde(hakukohdeOid).get(5, TimeUnit.MINUTES);
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
      HakukohdeImportDTO importTyyppi = new HakukohdeImportDTO();

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

      List<HakukohteenValintakoeDTO> valintakoeDTOs = new ArrayList<>();
      for (Valintakoe valintakoe : hakukohde.valintakokeet) {
        HakukohteenValintakoeDTO v = new HakukohteenValintakoeDTO();
        v.setOid(valintakoe.id);
        v.setTyyppiUri(valintakoe.valintakokeentyyppiUri);
        valintakoeDTOs.add(v);
      }

      List<HakukohteenValintakoeDTO> uniqueValintakokeet =
          valintakoeDTOs.stream()
              .collect(Collectors.groupingBy(HakukohteenValintakoeDTO::getTyyppiUri))
              .values()
              .stream()
              .map(v -> v.get(0))
              .collect(Collectors.toList());
      importTyyppi.setValintakoe(uniqueValintakokeet);

      String hakukohdeKoodiTunniste = hakukohde.oid.replace(".", "_");

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

      HakukohdeValintaperusteetDTO valintaperusteet =
          tarjontaAsyncResource.findValintaperusteetByOid(hakukohdeOid).get(60, TimeUnit.SECONDS);

      // Tänne mennään vain jos tiedot löytyvät vanhasta tarjonnasta
      if (valintaperusteet != null) {

        importTyyppi.setValintakoe(new ArrayList<>());
        for (ValintakoeDTO valintakoeDTO : valintaperusteet.getValintakokeet()) {
          HakukohteenValintakoeDTO v = new HakukohteenValintakoeDTO();
          v.setOid(valintakoeDTO.getOid());
          v.setTyyppiUri(valintakoeDTO.getTyyppiUri());
          importTyyppi.getValintakoe().add(v);
        }

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("paasykoe_min");
        avainArvo.setArvo(valintaperusteet.getPaasykoeMin().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("paasykoe_max");
        avainArvo.setArvo(valintaperusteet.getPaasykoeMax().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("paasykoe_hylkays_min");
        avainArvo.setArvo(valintaperusteet.getPaasykoeHylkaysMin().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("paasykoe_hylkays_max");
        avainArvo.setArvo(valintaperusteet.getPaasykoeHylkaysMax().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("lisanaytto_min");
        avainArvo.setArvo(valintaperusteet.getLisanayttoMin().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("lisanaytto_max");
        avainArvo.setArvo(valintaperusteet.getLisanayttoMax().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("lisanaytto_hylkays_min");
        avainArvo.setArvo(valintaperusteet.getLisanayttoHylkaysMin().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("lisanaytto_hylkays_max");
        avainArvo.setArvo(valintaperusteet.getLisanayttoHylkaysMax().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("paasykoe_ja_lisanaytto_hylkays_min");
        avainArvo.setArvo(valintaperusteet.getHylkaysMin().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("paasykoe_ja_lisanaytto_hylkays_max");
        avainArvo.setArvo(valintaperusteet.getHylkaysMax().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("painotettu_keskiarvo_hylkays_min");
        avainArvo.setArvo(valintaperusteet.getPainotettuKeskiarvoHylkaysMin().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("painotettu_keskiarvo_hylkays_max");
        avainArvo.setArvo(valintaperusteet.getPainotettuKeskiarvoHylkaysMax().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("paasykoe_tunniste");
        avainArvo.setArvo(
            valintaperusteet.getPaasykoeTunniste() != null
                ? valintaperusteet.getPaasykoeTunniste()
                : hakukohdeKoodiTunniste + "_paasykoe");
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("lisanaytto_tunniste");
        avainArvo.setArvo(
            valintaperusteet.getLisanayttoTunniste() != null
                ? valintaperusteet.getLisanayttoTunniste()
                : hakukohdeKoodiTunniste + "_lisanaytto");
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("lisapiste_tunniste");
        avainArvo.setArvo(
            valintaperusteet.getLisapisteTunniste() != null
                ? valintaperusteet.getLisapisteTunniste()
                : hakukohdeKoodiTunniste + "_lisapiste");
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("urheilija_lisapiste_tunniste");
        avainArvo.setArvo(
            valintaperusteet.getUrheilijaLisapisteTunniste() != null
                ? valintaperusteet.getUrheilijaLisapisteTunniste()
                : hakukohdeKoodiTunniste + "_urheilija_lisapiste");
        importTyyppi.getValintaperuste().add(avainArvo);

        for (String avain : valintaperusteet.getPainokertoimet().keySet()) {
          avainArvo = new AvainArvoDTO();
          avainArvo.setAvain(avain);
          avainArvo.setArvo(valintaperusteet.getPainokertoimet().get(avain));
          importTyyppi.getValintaperuste().add(avainArvo);
        }
      }

      return importTyyppi;
    } catch (Exception e) {
      String msg = String.format("Importointi hakukohteelle %s epaonnistui!", hakukohdeOid);
      LOG.error(msg, e);
      throw new RuntimeException(msg, e);
    }
  }
}
