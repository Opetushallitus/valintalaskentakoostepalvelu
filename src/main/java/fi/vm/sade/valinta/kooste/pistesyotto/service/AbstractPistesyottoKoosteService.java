package fi.vm.sade.valinta.kooste.pistesyotto.service;

import static org.apache.commons.collections.ListUtils.union;

import com.google.common.collect.Sets;
import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeCreateDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.Organisaatio;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.AbstractHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Osallistuminen;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HakemuksenKoetulosYhteenveto;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoDataRiviKuuntelija;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.sharedutils.AuditLog;
import fi.vm.sade.valinta.sharedutils.ValintaResource;
import fi.vm.sade.valinta.sharedutils.ValintaperusteetOperation;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;
import io.reactivex.observables.ConnectableObservable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPistesyottoKoosteService {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractPistesyottoKoosteService.class);

  private static final Set<String> KIELIKOKEEN_SALLITUT_OSALLISTUMISET_ILMAN_TULOSTA =
      Set.of(
          Osallistuminen.EI_OSALLISTUNUT.name(),
          Osallistuminen.MERKITSEMATTA.name(),
          Osallistuminen.EI_VAADITA.name());

  protected final ApplicationAsyncResource applicationAsyncResource;
  protected final AtaruAsyncResource ataruAsyncResource;
  protected final TarjontaAsyncResource tarjontaAsyncResource;
  protected final OrganisaatioAsyncResource organisaatioAsyncResource;
  protected final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
  protected final ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource;
  protected final ValintapisteAsyncResource valintapisteAsyncResource;

  protected AbstractPistesyottoKoosteService(
      ApplicationAsyncResource applicationAsyncResource,
      AtaruAsyncResource ataruAsyncResource,
      ValintapisteAsyncResource valintapisteAsyncResource,
      TarjontaAsyncResource tarjontaAsyncResource,
      OrganisaatioAsyncResource organisaatioAsyncResource,
      ValintaperusteetAsyncResource valintaperusteetAsyncResource,
      ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource) {
    this.applicationAsyncResource = applicationAsyncResource;
    this.ataruAsyncResource = ataruAsyncResource;
    this.valintapisteAsyncResource = valintapisteAsyncResource;
    this.tarjontaAsyncResource = tarjontaAsyncResource;
    this.organisaatioAsyncResource = organisaatioAsyncResource;
    this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
    this.valintalaskentaValintakoeAsyncResource = valintalaskentaValintakoeAsyncResource;
  }

  private PistesyottoExcel muodostoPistesyottoExcel(
      String hakuOid,
      String hakukohdeOid,
      Optional<String> aikaleima,
      List<ValintakoeOsallistuminenDTO> osallistumistiedot,
      List<HakemusWrapper> hakemukset,
      List<ValintaperusteDTO> valintaperusteet,
      List<ApplicationAdditionalDataDTO> pistetiedot,
      List<HakukohdeJaValintakoeDTO> hakukohdeJaValintakoe,
      Haku haku,
      AbstractHakukohde hakukohdeDTO,
      List<Organisaatio> tarjoajat,
      Collection<PistesyottoDataRiviKuuntelija> kuuntelijat) {
    String hakuNimi = new Teksti(haku.nimi).getTeksti();
    String tarjoajaOid = hakukohdeDTO.tarjoajaOids.iterator().next();
    String hakukohdeNimi = new Teksti(hakukohdeDTO.nimi).getTeksti();
    String tarjoajaNimi =
        Teksti.getTeksti(
            tarjoajat.stream().map(Organisaatio::getNimi).collect(Collectors.toList()), " - ");
    Collection<String> valintakoeTunnisteet =
        valintaperusteet.stream().map(ValintaperusteDTO::getTunniste).collect(Collectors.toList());

    Set<String> kaikkiKutsutaanTunnisteet =
        hakukohdeJaValintakoe.stream()
            .flatMap(h -> h.getValintakoeDTO().stream())
            .filter(v -> Boolean.TRUE.equals(v.getKutsutaankoKaikki()))
            .map(ValintakoeCreateDTO::getTunniste)
            .collect(Collectors.toSet());

    return new PistesyottoExcel(
        hakuOid,
        hakukohdeOid,
        tarjoajaOid,
        hakuNimi,
        hakukohdeNimi,
        tarjoajaNimi,
        aikaleima,
        hakemukset,
        kaikkiKutsutaanTunnisteet,
        valintakoeTunnisteet,
        osallistumistiedot,
        valintaperusteet,
        pistetiedot,
        kuuntelijat);
  }

  protected Observable<List<HakemusWrapper>> getHakemuksetByOids(List<String> hakemusOids) {
    return Observable.fromFuture(ataruAsyncResource.getApplicationsByOids(hakemusOids))
        .flatMap(
            hakemukset -> {
              if (hakemukset.isEmpty()) {
                return applicationAsyncResource.getApplicationsByHakemusOids(hakemusOids);
              } else {
                return Observable.just(hakemukset);
              }
            });
  }

  private CompletableFuture<List<HakemusWrapper>> getHakemukset(Haku haku, String hakukohdeOid) {
    if (haku.isHakemuspalvelu()) {
      return ataruAsyncResource.getApplicationsByHakukohde(hakukohdeOid);
    } else {
      return applicationAsyncResource.getApplicationsByOid(haku.oid, hakukohdeOid);
    }
  }

  protected Observable<
          Triple<
              PistesyottoExcel,
              Map<String, ApplicationAdditionalDataDTO>,
              Map<String, HakemusWrapper>>>
      muodostaPistesyottoExcel(
          String hakuOid,
          String hakukohdeOid,
          AuditSession auditSession,
          DokumenttiProsessi prosessi,
          Collection<PistesyottoDataRiviKuuntelija> kuuntelijat) {
    BiFunction<
            List<ValintakoeOsallistuminenDTO>,
            PisteetWithLastModified,
            Observable<PisteetWithLastModified>>
        haePuuttuvatLisatiedot =
            (osallistumiset, lisatiedot) -> {
              final Map<String, ValintakoeOsallistuminenDTO> osallistuminenByHakemusOID =
                  osallistumiset.stream().collect(Collectors.toMap(o -> o.getHakemusOid(), o -> o));
              Sets.SetView<String> puuttuvatLisatiedot =
                  Sets.difference(
                      osallistuminenByHakemusOID.keySet(),
                      lisatiedot.valintapisteet.stream()
                          .map(l -> l.getHakemusOID())
                          .collect(Collectors.toSet()));
              if (puuttuvatLisatiedot.isEmpty()) {
                return Observable.just(lisatiedot);
              }

              prosessi.inkrementoiKokonaistyota();
              Function<Valintapisteet, Valintapisteet> populateNameAndOppijaOID =
                  v -> {
                    ValintakoeOsallistuminenDTO o =
                        osallistuminenByHakemusOID.get(v.getHakemusOID());
                    return new Valintapisteet(
                        v.getHakemusOID(),
                        o.getHakijaOid(),
                        o.getEtunimi(),
                        o.getSukunimi(),
                        v.getPisteet());
                  };

              return Observable.fromFuture(
                      valintapisteAsyncResource.getValintapisteetWithHakemusOidsAsFuture(
                          new ArrayList<>(puuttuvatLisatiedot), auditSession))
                  .map(
                      v ->
                          v.valintapisteet.stream()
                              .map(populateNameAndOppijaOID)
                              .collect(Collectors.toList()))
                  .map(
                      p ->
                          new PisteetWithLastModified(
                              lisatiedot.lastModified, union(lisatiedot.valintapisteet, p)))
                  .doOnComplete(prosessi::inkrementoiTehtyjaToita);
            };
    BiFunction<
            List<ValintakoeOsallistuminenDTO>,
            List<HakemusWrapper>,
            Observable<List<HakemusWrapper>>>
        haePuuttuvatHakemukset =
            (osallistumiset, hakemukset) -> {
              Set<String> puuttuvatHakemukset =
                  osallistumiset.stream()
                      .map(ValintakoeOsallistuminenDTO::getHakemusOid)
                      .collect(Collectors.toSet());
              puuttuvatHakemukset.removeAll(
                  hakemukset.stream().map(HakemusWrapper::getOid).collect(Collectors.toSet()));
              if (puuttuvatHakemukset.isEmpty()) {
                return Observable.just(hakemukset);
              }
              LOG.warn(
                  "Valintakokeen osallistumisissa oli hakemuksia, joita ei löytynyt hakukohteelta: {}",
                  String.join(", ", puuttuvatHakemukset));
              prosessi.inkrementoiKokonaistyota();
              return getHakemuksetByOids(new ArrayList<>(puuttuvatHakemukset))
                  .map(
                      hs ->
                          Stream.concat(hakemukset.stream(), hs.stream())
                              .collect(Collectors.toList()))
                  .doOnComplete(prosessi::inkrementoiTehtyjaToita);
            };
    ConnectableObservable<List<ValintaperusteDTO>> kokeetO =
        Observable.fromFuture(valintaperusteetAsyncResource.findAvaimet(hakukohdeOid)).replay(1);
    ConnectableObservable<List<ValintakoeOsallistuminenDTO>> osallistumistiedotO =
        Observable.fromFuture(valintalaskentaValintakoeAsyncResource.haeHakutoiveelle(hakukohdeOid))
            .replay(1);

    Observable<Haku> hakuO = Observable.fromFuture(tarjontaAsyncResource.haeHaku(hakuOid));
    Observable<List<HakemusWrapper>> hakemuksetO =
        Observable.merge(
            Observable.zip(
                osallistumistiedotO,
                hakuO.flatMap(haku -> Observable.fromFuture(getHakemukset(haku, hakukohdeOid))),
                haePuuttuvatHakemukset));

    Observable<PisteetWithLastModified> pisteetWithLastModifiedObservable =
        hakemuksetO.flatMap(
            hakemusWrappers -> {
              List<String> hakemusOids =
                  hakemusWrappers.stream().map(HakemusWrapper::getOid).collect(Collectors.toList());
              return Observable.fromFuture(
                  valintapisteAsyncResource.getValintapisteetWithHakemusOidsAsFuture(
                      hakemusOids, auditSession));
            });

    Observable<PisteetWithLastModified> merge =
        Observable.merge(
            Observable.zip(
                osallistumistiedotO, pisteetWithLastModifiedObservable, haePuuttuvatLisatiedot));
    Observable<Pair<Optional<String>, List<ApplicationAdditionalDataDTO>>> lisatiedotO =
        Observable.zip(
            merge,
            kokeetO,
            osallistumistiedotO,
            (lisatiedot, kokeet, osallistumistiedot) -> {
              Map<String, ValintakoeOsallistuminenDTO> osallistumistiedotByHakemusOid =
                  osallistumistiedot.stream()
                      .collect(
                          Collectors.toMap(ValintakoeOsallistuminenDTO::getHakemusOid, o -> o));
              return Pair.of(
                  lisatiedot.lastModified,
                  lisatiedot.valintapisteet.stream()
                      .map(
                          l ->
                              new HakemuksenKoetulosYhteenveto(
                                      l,
                                      Pair.of(hakukohdeOid, kokeet),
                                      osallistumistiedotByHakemusOid.get(l.getHakemusOID()))
                                  .applicationAdditionalDataDTO)
                      .collect(Collectors.toList()));
            });
    CompletableFuture<AbstractHakukohde> hakukohdeF =
        tarjontaAsyncResource.haeHakukohde(hakukohdeOid);
    CompletableFuture<List<Organisaatio>> tarjoajatF =
        hakukohdeF.thenComposeAsync(
            hakukohde ->
                CompletableFutureUtil.sequence(
                    hakukohde.tarjoajaOids.stream()
                        .map(organisaatioAsyncResource::haeOrganisaatio)
                        .collect(Collectors.toList())));

    prosessi.inkrementoiKokonaistyota();
    kokeetO.connect();
    osallistumistiedotO.connect();
    LOG.info("Palautetaan yhdistävä excelinmuodostus-Observable prosessille {}", prosessi.getId());
    return Observable.zip(
            osallistumistiedotO,
            lisatiedotO,
            hakemuksetO,
            kokeetO,
            valintaperusteetAsyncResource.haeValintakokeetHakukohteille(
                Collections.singletonList(hakukohdeOid)),
            Observable.fromFuture(hakukohdeF),
            Observable.fromFuture(tarjoajatF),
            hakuO,
            (osallistumistiedot,
                lisatiedot,
                hakemukset,
                kokeet,
                valintakoeosallistumiset,
                hakukohde,
                tarjoajat,
                haku) ->
                Triple.of(
                    muodostoPistesyottoExcel(
                        hakuOid,
                        hakukohdeOid,
                        lisatiedot.getKey(),
                        osallistumistiedot,
                        hakemukset,
                        kokeet,
                        lisatiedot.getRight(),
                        valintakoeosallistumiset,
                        haku,
                        hakukohde,
                        tarjoajat,
                        kuuntelijat),
                    lisatiedot.getRight().stream()
                        .collect(Collectors.toMap(ApplicationAdditionalDataDTO::getOid, l -> l)),
                    hakemukset.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h))))
        .doOnComplete(prosessi::inkrementoiTehtyjaToita);
  }

  protected CompletableFuture<Set<String>> tallennaKoostetutPistetiedot(
      String hakuOid,
      String hakukohdeOid,
      Optional<String> ifUnmodifiedSince,
      List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle,
      ValintaperusteetOperation auditLogOperation,
      AuditSession auditSession) {
    try {
      pistetiedotHakemukselle.forEach(
          AbstractPistesyottoKoosteService::tarkistaKielikoepistetiedot);
    } catch (IllegalArgumentException e) {
      return CompletableFuture.failedFuture(e);
    }
    return tallennaPisteetValintaPisteServiceen(
            hakuOid,
            hakukohdeOid,
            ifUnmodifiedSince,
            pistetiedotHakemukselle,
            auditLogOperation,
            auditSession)
        .whenComplete(
            (r, t) -> {
              if (t != null) {
                throw new IllegalStateException(
                    String.format(
                        "Virhe tallennettaessa koostettuja pistetietoja haun %s hakukohteelle %s",
                        hakuOid, hakukohdeOid),
                    t);
              }
            });
  }

  static void tarkistaKielikoepistetiedot(ApplicationAdditionalDataDTO pistetieto) {
    Map<String, String> additionalData = pistetieto.getAdditionalData();
    additionalData.keySet().stream()
        .filter(avain -> avain.matches(PistesyottoExcel.KIELIKOE_REGEX))
        .forEach(
            avain -> {
              String osallistuminen =
                  additionalData.get(Valintapisteet.withOsallistuminenSuffix(avain));
              if (Osallistuminen.OSALLISTUI.name().equals(osallistuminen)) {
                String tulos = additionalData.get(avain);
                if (!"true".equals(tulos) && !"false".equals(tulos)) {
                  throw new IllegalArgumentException(
                      String.format(
                          "Huono kielikokeen %s tulos '%s' hakemuksella %s",
                          avain, tulos, pistetieto.getOid()));
                }
              } else if (osallistuminen == null
                  || !KIELIKOKEEN_SALLITUT_OSALLISTUMISET_ILMAN_TULOSTA.contains(osallistuminen)) {
                throw new IllegalArgumentException(
                    String.format(
                        "Huono kielikokeen %s osallistumistieto '%s' hakemuksella %s",
                        avain, osallistuminen, pistetieto.getOid()));
              }
            });
  }

  private List<Valintapisteet> pistetiedotHakemukselle(
      String tallettaja, List<ApplicationAdditionalDataDTO> additionalDataDTOS) {
    return additionalDataDTOS.stream()
        .map(a -> Pair.of(tallettaja, a))
        .map(Valintapisteet::new)
        .collect(Collectors.toList());
  }

  private CompletableFuture<Set<String>> tallennaPisteetValintaPisteServiceen(
      String hakuOid,
      String hakukohdeOid,
      Optional<String> ifUnmodifiedSince,
      List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle,
      ValintaperusteetOperation auditLogOperation,
      AuditSession auditSession) {
    return valintapisteAsyncResource
        .putValintapisteet(
            ifUnmodifiedSince,
            pistetiedotHakemukselle(auditSession.getPersonOid(), pistetiedotHakemukselle),
            auditSession)
        .whenComplete(
            (conflictingHakemusOids, exception) -> {
              if (exception != null) {
                throw new IllegalStateException(
                    "Lisätietojen tallennus hakemukselle epäonnistui", exception);
              } else {
                pistetiedotHakemukselle.forEach(
                    pistetieto -> {
                      Map<String, String> additionalInfo = new HashMap<>();
                      additionalInfo.put("Username from call params", auditSession.getPersonOid());
                      additionalInfo.put("hakuOid", hakuOid);
                      additionalInfo.put("hakukohdeOid", hakukohdeOid);
                      additionalInfo.put("hakijaOid", pistetieto.getPersonOid());
                      AuditLog.log(
                          KoosteAudit.AUDIT,
                          auditSession.asAuditUser(),
                          auditLogOperation,
                          ValintaResource.PISTESYOTTOSERVICE,
                          pistetieto.getOid(),
                          Changes.addedDto(pistetieto),
                          additionalInfo);
                    });
              }
            });
  }
}
