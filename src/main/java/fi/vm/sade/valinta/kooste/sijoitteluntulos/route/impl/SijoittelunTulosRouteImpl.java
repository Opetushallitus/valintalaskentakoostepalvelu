package fi.vm.sade.valinta.kooste.sijoitteluntulos.route.impl;

import static fi.vm.sade.valinta.kooste.security.SecurityPreprocessor.SECURITY;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.camel.LoggingLevel.ERROR;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakemusDTO;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.Organisaatio;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.AbstractHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Lukuvuosimaksu;
import fi.vm.sade.valinta.kooste.sijoittelu.exception.SijoittelultaEiSisaltoaPoikkeus;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.Tiedosto;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.Valmis;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosOsoitetarratRoute;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosTaulukkolaskentaRoute;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohteetTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.util.NimiPaattelyStrategy;
import fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Varoitus;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.SijoittelunTulosExcelKomponentti;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRouteBuilder;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import io.reactivex.Observable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Component
public class SijoittelunTulosRouteImpl extends AbstractDokumenttiRouteBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(SijoittelunTulosRouteImpl.class);

  private final long getTimeToLive() {
    return DateTime.now().plusHours(720).toDate().getTime();
  }

  private final boolean pakkaaTiedostotTarriin;
  private final HaeHakukohteetTarjonnaltaKomponentti hakukohteetTarjonnalta;
  private final SijoittelunTulosExcelKomponentti sijoittelunTulosExcel;
  private final DokumenttiAsyncResource dokumenttiAsyncResource;
  private final ViestintapalveluResource viestintapalveluResource;
  private final ApplicationResource applicationResource;
  private final String hakukohteidenHaku;
  private final String luontiEpaonnistui;
  private final String taulukkolaskenta;
  private final String osoitetarrat;
  private final String dokumenttipalveluUrl;
  private final String muodostaDokumentit;
  private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;
  private final TarjontaAsyncResource tarjontaAsyncResource;
  private final OrganisaatioAsyncResource organisaatioAsyncResource;
  private final AtaruAsyncResource ataruAsyncResource;
  private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;
  private final ValintalaskentaAsyncResource valintalaskentaResource;

  @Autowired
  public SijoittelunTulosRouteImpl(
      @Value(
              "${valintalaskentakoostepalvelu.sijoittelunTulosRouteImpl.pakkaaTiedostotTarriin:false}")
          boolean pakkaaTiedostotTarriin,
      @Value("${valintalaskentakoostepalvelu.dokumenttipalvelu.rest.url}/dokumentit/lataa/")
          String dokumenttipalveluUrl,
      @Value(SijoittelunTulosTaulukkolaskentaRoute.SEDA_SIJOITTELUNTULOS_TAULUKKOLASKENTA_HAULLE)
          String taulukkolaskenta,
      @Value(SijoittelunTulosOsoitetarratRoute.SEDA_SIJOITTELUNTULOS_OSOITETARRAT_HAULLE)
          String osoitetarrat,
      KoodistoCachedAsyncResource koodistoCachedAsyncResource,
      HaeHakukohteetTarjonnaltaKomponentti hakukohteetTarjonnalta,
      SijoittelunTulosExcelKomponentti sijoittelunTulosExcel,
      TarjontaAsyncResource tarjontaAsyncResource,
      OrganisaatioAsyncResource organisaatioAsyncResource,
      ViestintapalveluResource viestintapalveluResource,
      ApplicationResource applicationResource,
      AtaruAsyncResource ataruAsyncResource,
      DokumenttiAsyncResource dokumenttiAsyncResource,
      ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource,
      ValintalaskentaAsyncResource valintalaskentaResource) {
    this.tarjontaAsyncResource = tarjontaAsyncResource;
    this.organisaatioAsyncResource = organisaatioAsyncResource;
    this.ataruAsyncResource = ataruAsyncResource;
    this.valintaTulosServiceAsyncResource = valintaTulosServiceAsyncResource;
    this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
    this.pakkaaTiedostotTarriin = pakkaaTiedostotTarriin;
    this.applicationResource = applicationResource;
    this.osoitetarrat = osoitetarrat;
    this.viestintapalveluResource = viestintapalveluResource;
    this.dokumenttipalveluUrl = dokumenttipalveluUrl;
    this.muodostaDokumentit = "direct:sijoitteluntulos_muodosta_dokumentit";
    this.hakukohteidenHaku = "direct:sijoitteluntulos_hakukohteiden_haku";
    this.luontiEpaonnistui = "direct:sijoitteluntulos_koko_haulle_deadletterchannel";
    this.hakukohteetTarjonnalta = hakukohteetTarjonnalta;
    this.sijoittelunTulosExcel = sijoittelunTulosExcel;
    this.dokumenttiAsyncResource = dokumenttiAsyncResource;
    this.taulukkolaskenta = taulukkolaskenta;
    this.valintalaskentaResource = valintalaskentaResource;
  }

  public void configure() {
    configureMuodostaDokumentit();
    configureDeadLetterChannel();
    configureHakukohteidenHaku();
    configureTaulukkolaskenta();
    configureOsoitetarrat();
  }

  private void configureTaulukkolaskenta() {
    String yksittainenTaulukkoTyo = yksittainenTyo("taulukkolaskenta");
    from(taulukkolaskenta)
        .errorHandler(
            deadLetterChannel(luontiEpaonnistui)
                .maximumRedeliveries(0)
                .logExhaustedMessageHistory(true)
                .logExhausted(true)
                .logStackTrace(true)
                // hide retry/handled stacktrace
                .logRetryStackTrace(false)
                .logHandled(false))
        .log(ERROR, "Aloitetaan taulukkolaskentojen muodostus koko haulle!")
        .process(SECURITY)
        .to(hakukohteidenHaku)
        .split(body())
        .stopOnException()
        .shareUnitOfWork()
        .to(yksittainenTaulukkoTyo)
        .end();
    from(yksittainenTaulukkoTyo)
        .routeId("Sijoitteluntulokset koko haulle taulukkolaskentatyöjono")
        .process(SECURITY)
        .process(
            new Processor() {
              @Override
              public void process(Exchange exchange) {
                SijoittelunTulosProsessi prosessi = prosessi(exchange);
                String hakukohdeOid = exchange.getIn().getBody(HakukohdeTyyppi.class).getOid();
                String hakuOid = hakuOid(exchange);
                String tarjoajaOid = StringUtils.EMPTY;
                AuditSession auditSession = auditSession(exchange);
                String hakukohdeNimi;
                String tarjoajaNimi;
                String preferoitukielikoodi = KieliUtil.SUOMI;
                try {
                  AbstractHakukohde hakukohde =
                      tarjontaAsyncResource.haeHakukohde(hakukohdeOid).get(5, MINUTES);
                  tarjoajaOid = hakukohde.tarjoajaOids.iterator().next();
                  Teksti hakukohdeTeksti = new Teksti(hakukohde.nimi);
                  preferoitukielikoodi = hakukohdeTeksti.getKieli();
                  hakukohdeNimi = hakukohdeTeksti.getTeksti();
                  tarjoajaNimi =
                      Teksti.getTeksti(
                          CompletableFutureUtil.sequence(
                                  hakukohde.tarjoajaOids.stream()
                                      .map(organisaatioAsyncResource::haeOrganisaatio)
                                      .collect(Collectors.toList()))
                              .get(5, MINUTES)
                              .stream()
                              .map(Organisaatio::getNimi)
                              .collect(Collectors.toList()),
                          " - ");
                } catch (Exception e) {
                  hakukohdeNimi = "Nimetön hakukohde " + hakukohdeOid;
                  tarjoajaNimi = "Nimetön tarjoaja " + tarjoajaOid;
                  prosessi
                      .getVaroitukset()
                      .add(new Varoitus(hakukohdeOid, "Hakukohteelle ei saatu tarjoajaOidia!"));
                }
                List<Valintatulos> tilat = Collections.emptyList();
                List<HakemusWrapper> hakemukset = Collections.emptyList();
                List<Lukuvuosimaksu> lukuvuosimaksus = Collections.emptyList();
                fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO hk = null;
                Haku hakuDTO = null;
                List<ValintatietoValinnanvaiheDTO> valinnanvaiheet = ListUtils.EMPTY_LIST;
                try {
                  hakuDTO = tarjontaAsyncResource.haeHaku(hakuOid).get(5, TimeUnit.MINUTES);
                  // TODO here it would make more sense to parallelise the asynchronous calls and
                  // bundle the results together after they all complete.
                  hakemukset =
                      (hakuDTO.isHakemuspalvelu()
                              ? Observable.fromFuture(
                                  ataruAsyncResource.getApplicationsByHakukohde(hakukohdeOid))
                              : Observable.just(
                                  applicationResource
                                      .getApplicationsByOid(
                                          hakuOid,
                                          hakukohdeOid,
                                          ApplicationResource.ACTIVE_AND_INCOMPLETE,
                                          ApplicationResource.MAX)
                                      .stream()
                                      .<HakemusWrapper>map(HakuappHakemusWrapper::new)
                                      .collect(Collectors.toList())))
                          .timeout(5, MINUTES)
                          .blockingFirst();
                  hk =
                      valintaTulosServiceAsyncResource
                          .getHakukohdeBySijoitteluajoPlainDTO(hakuOid, hakukohdeOid)
                          .timeout(5, MINUTES)
                          .toFuture()
                          .get();
                  lukuvuosimaksus =
                      valintaTulosServiceAsyncResource
                          .fetchLukuvuosimaksut(hakukohdeOid, auditSession)
                          .timeout(5, MINUTES)
                          .toFuture()
                          .get();
                  valinnanvaiheet =
                      valintalaskentaResource.laskennantulokset(hakukohdeOid).get(1, MINUTES);
                } catch (Exception e) {
                  log.error("Problem when getting resources for creating the excel", e);
                }

                try {
                  if (pakkaaTiedostotTarriin) {
                    Tiedosto tiedosto =
                        new Tiedosto(
                            "sijoitteluntulos_" + hakukohdeOid + ".xlsx",
                            IOUtils.toByteArray(
                                sijoittelunTulosExcel.luoXls(
                                    tilat,
                                    preferoitukielikoodi,
                                    hakukohdeNimi,
                                    tarjoajaNimi,
                                    hakukohdeOid,
                                    hakemukset,
                                    lukuvuosimaksus,
                                    hk,
                                    hakuDTO,
                                    valinnanvaiheet)));
                    prosessi.getValmiit().add(new Valmis(tiedosto, hakukohdeOid, tarjoajaOid));
                    return;
                  } else {
                    InputStream input =
                        sijoittelunTulosExcel.luoXls(
                            tilat,
                            preferoitukielikoodi,
                            hakukohdeNimi,
                            tarjoajaNimi,
                            hakukohdeOid,
                            hakemukset,
                            lukuvuosimaksus,
                            hk,
                            hakuDTO,
                            valinnanvaiheet);
                    try {
                      String id = generateId();
                      String finalTarjoajaOid = tarjoajaOid;
                      dokumenttiAsyncResource
                          .tallenna(
                              id,
                              "sijoitteluntulos_" + hakukohdeOid + ".xlsx",
                              getTimeToLive(),
                              dokumenttiprosessi(exchange).getTags(),
                              "application/vnd.ms-excel",
                              input)
                          .subscribe(
                              ok -> {
                                prosessi
                                    .getValmiit()
                                    .add(new Valmis(hakukohdeOid, finalTarjoajaOid, id));
                              },
                              poikkeus -> {
                                LOG.error(
                                    "Sijoittelun tulosexcelin tallennus dokumenttipalveluun epäonnistui");
                                throw new RuntimeException(poikkeus);
                              });
                    } catch (Exception e) {
                      LOG.error(
                          "Dokumentin tallennus epäonnistui hakukohteelle " + hakukohdeOid, e);
                      prosessi
                          .getVaroitukset()
                          .add(
                              new Varoitus(
                                  hakukohdeOid,
                                  "Ei saatu tallennettua dokumenttikantaan! " + e.getMessage()));
                      prosessi.getValmiit().add(new Valmis(hakukohdeOid, tarjoajaOid, null));
                    }
                  }
                  LOG.info("Sijoitteluntulosexcelin luonti onnistui hakukohteelle " + hakukohdeOid);
                } catch (SijoittelultaEiSisaltoaPoikkeus e) {
                  prosessi.getValmiit().add(new Valmis(hakukohdeOid, tarjoajaOid, null, true));
                } catch (Exception e) {
                  LOG.error(
                      "Sijoitteluntulosexcelin luonti epäonnistui hakukohteelle " + hakukohdeOid,
                      e);
                  prosessi
                      .getVaroitukset()
                      .add(
                          new Varoitus(
                              hakukohdeOid,
                              "Ei saatu sijoittelun tuloksia tai hakukohteita! " + e.getMessage()));
                  prosessi.getValmiit().add(new Valmis(hakukohdeOid, tarjoajaOid, null));
                }
              }
            })
        .to(muodostaDokumentit);
  }

  private void configureOsoitetarrat() {
    String yksittainenOsoitetarraTyo = yksittainenTyo("osoitetarrat");
    from(osoitetarrat)
        .errorHandler(
            deadLetterChannel(luontiEpaonnistui)
                .maximumRedeliveries(0)
                .logExhaustedMessageHistory(true)
                .logExhausted(true)
                .logStackTrace(true)
                // hide retry/handled stacktrace
                .logRetryStackTrace(false)
                .logHandled(false))
        .log(ERROR, "Aloitetaan osoitetarrojen muodostus koko haulle!")
        .process(SECURITY)
        .to(hakukohteidenHaku)
        .split(body())
        .stopOnException()
        .shareUnitOfWork()
        .to(yksittainenOsoitetarraTyo)
        .end();
    from(yksittainenOsoitetarraTyo)
        .routeId("Sijoitteluntulokset koko haulle osoitetarrattyöjono")
        .process(SECURITY)
        .process(
            new Processor() {
              @Override
              public void process(Exchange exchange) {
                SijoittelunTulosProsessi prosessi = prosessi(exchange);
                HakukohdeTyyppi hakukohde = exchange.getIn().getBody(HakukohdeTyyppi.class);
                String hakukohdeOid = hakukohde.getOid();
                StopWatch stopWatch =
                    new StopWatch("Hakukohteen " + hakukohdeOid + " osoitetarrojen muodostus");
                stopWatch.start("Tiedot tarjonnasta");
                String tarjoajaOid = StringUtils.EMPTY;
                try {
                  tarjoajaOid =
                      tarjontaAsyncResource
                          .haeHakukohde(hakukohdeOid)
                          .get(5, MINUTES)
                          .tarjoajaOids
                          .iterator()
                          .next();
                } catch (Exception e) {
                  prosessi
                      .getVaroitukset()
                      .add(new Varoitus(hakukohdeOid, "Hakukohteelle ei saatu tarjoajaOidia!"));
                }
                stopWatch.stop();
                try {
                  stopWatch.start("Tiedot koodistosta");
                  Map<String, Koodi> maajavaltio =
                      koodistoCachedAsyncResource.haeKoodisto(
                          KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
                  Map<String, Koodi> posti =
                      koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI);
                  stopWatch.stop();
                  stopWatch.start("Tiedot valintarekisteristä");
                  fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO hakukohteenTulos =
                      valintaTulosServiceAsyncResource
                          .getHakukohdeBySijoitteluajoPlainDTO(hakuOid(exchange), hakukohdeOid)
                          .timeout(5, MINUTES)
                          .toFuture()
                          .get();
                  stopWatch.stop();
                  List<String> hyvaksytytHakemukset =
                      hakukohteenTulos.getValintatapajonot().stream()
                          .flatMap(valintatapajono -> valintatapajono.getHakemukset().stream())
                          .filter(hakemus -> hakemus.getTila().isHyvaksytty())
                          .map(HakemusDTO::getHakemusOid)
                          .distinct()
                          .collect(Collectors.toList());
                  if (hyvaksytytHakemukset.isEmpty()) {
                    prosessi.getValmiit().add(new Valmis(hakukohdeOid, tarjoajaOid, null, true));
                    return;
                  }
                  stopWatch.start("Tiedot hakemuksilta");
                  List<HakemusWrapper> hakemukset =
                      Observable.fromFuture(tarjontaAsyncResource.haeHaku(hakuOid(exchange)))
                          .flatMap(
                              haku ->
                                  haku.isHakemuspalvelu()
                                      ? Observable.fromFuture(
                                          ataruAsyncResource.getApplicationsByOids(
                                              hyvaksytytHakemukset))
                                      : Observable.just(
                                          applicationResource
                                              .getApplicationsByOids(hyvaksytytHakemukset)
                                              .stream()
                                              .<HakemusWrapper>map(HakuappHakemusWrapper::new)
                                              .collect(Collectors.toList())))
                          .timeout(5, MINUTES)
                          .blockingFirst();
                  stopWatch.stop();
                  List<Osoite> addressLabels = Lists.newArrayList();

                  for (HakemusWrapper h : hakemukset) {
                    addressLabels.add(
                        OsoiteHakemukseltaUtil.osoiteHakemuksesta(
                            h, maajavaltio, posti, new NimiPaattelyStrategy()));
                  }
                  stopWatch.start("Tarrat viestintäpalvelulta");
                  Osoitteet osoitteet = new Osoitteet(addressLabels);
                  if (pakkaaTiedostotTarriin) {
                    Tiedosto tiedosto =
                        new Tiedosto(
                            "osoitetarrat_" + hakukohdeOid + ".pdf",
                            IOUtils.toByteArray(
                                viestintapalveluResource.haeOsoitetarratSync(osoitteet)));
                    prosessi.getValmiit().add(new Valmis(tiedosto, hakukohdeOid, tarjoajaOid));
                    return;
                  } else {
                    InputStream input =
                        pipeInputStreams(viestintapalveluResource.haeOsoitetarratSync(osoitteet));
                    String id = generateId();
                    String finalTarjoajaOid = tarjoajaOid;
                    dokumenttiAsyncResource
                        .tallenna(
                            id,
                            "osoitetarrat_" + hakukohdeOid + ".pdf",
                            getTimeToLive(),
                            dokumenttiprosessi(exchange).getTags(),
                            "application/pdf",
                            input)
                        .subscribe(
                            ok -> {
                              prosessi
                                  .getValmiit()
                                  .add(new Valmis(hakukohdeOid, finalTarjoajaOid, id));
                            },
                            poikkeus -> {
                              LOG.error("Osoitetarrojen tallennus dokumenttipalveluun epäonnistui");
                              throw new RuntimeException(poikkeus);
                            });
                  }
                  stopWatch.stop();
                  LOG.info(stopWatch.prettyPrint());
                  LOG.info("Osoitetarrojen luonti onnistui hakukohteelle " + hakukohdeOid);
                } catch (Exception e) {
                  LOG.error("Osoitetarrojen luonti epäonnistui hakukohteelle " + hakukohdeOid, e);
                  prosessi
                      .getVaroitukset()
                      .add(
                          new Varoitus(
                              hakukohdeOid,
                              "Ei saatu sijoittelun tuloksia tai hakukohteita! " + e.getMessage()));
                  prosessi.getValmiit().add(new Valmis(hakukohdeOid, tarjoajaOid, null));
                }
              }
            })
        .to(muodostaDokumentit);
  }

  private void configureDeadLetterChannel() {
    from(luontiEpaonnistui)
        .log(
            ERROR,
            "Sijoitteluntulosten taulukkolaskentaluonti epaonnistui: ${property.CamelExceptionCaught}")
        .process(
            new Processor() {
              @Override
              public void process(Exchange exchange) {
                if (dokumenttiprosessi(exchange).getPoikkeukset().isEmpty()) {
                  dokumenttiprosessi(exchange)
                      .getPoikkeukset()
                      .add(
                          new Poikkeus(
                              Poikkeus.KOOSTEPALVELU, "Sijoitteluntulosten vienti epäonnistui!"));
                }
              }
            })
        .stop();
  }

  private void configureMuodostaDokumentit() {
    from(muodostaDokumentit)
        .process(
            new Processor() {
              @Override
              public void process(Exchange exchange) {
                SijoittelunTulosProsessi prosessi = prosessi(exchange);
                if (prosessi.inkrementoi() == 0) {
                  try {
                    InputStream tar = generoiYhteenvetoTar(prosessi.getValmiit());
                    String id = generateId();
                    dokumenttiAsyncResource
                        .tallenna(
                            id,
                            "sijoitteluntuloksethaulle.tar",
                            defaultExpirationDate().getTime(),
                            dokumenttiprosessi(exchange).getTags(),
                            "application/x-tar",
                            tar)
                        .subscribe(
                            ok -> {
                              prosessi.setDokumenttiId(id);
                            },
                            poikkeus -> {
                              LOG.error(
                                  "Sijoittelun tulostietojen tallennus dokumenttipalveluun epäonnistui");
                              throw new RuntimeException(poikkeus);
                            });
                  } catch (Exception e) {
                    LOG.error("Tulostietojen tallennus dokumenttipalveluun epäonnistui!", e);
                    prosessi
                        .getPoikkeukset()
                        .add(
                            new Poikkeus(
                                Poikkeus.DOKUMENTTIPALVELU,
                                "Tulostietojen tallennus epäonnistui!"));
                  }
                }
              }
            });
  }

  private void configureHakukohteidenHaku() {
    from(hakukohteidenHaku)
        .errorHandler(
            deadLetterChannel(luontiEpaonnistui)
                .maximumRedeliveries(0)
                .logExhaustedMessageHistory(true)
                .logExhausted(true)
                .logStackTrace(true)
                // hide retry/handled stacktrace
                .logRetryStackTrace(false)
                .logHandled(false))
        .process(
            new Processor() {
              @Override
              public void process(Exchange exchange) throws Exception {
                String hakuOid = hakuOid(exchange);
                StopWatch stopWatch =
                    new StopWatch("Haun " + hakuOid + " hakukohteiden haku tarjonnasta");
                stopWatch.start();
                try {
                  dokumenttiprosessi(exchange)
                      .getVaroitukset()
                      .add(
                          new Varoitus(
                              hakuOid,
                              "Haetaan tarjonnalta kaikki hakukohteet! Varoitus, pyyntö saattaa kestää pitkään!"));
                  Collection<HakukohdeTyyppi> hakukohteet =
                      hakukohteetTarjonnalta.haeHakukohteetTarjonnalta(hakuOid);
                  if (hakukohteet == null || hakukohteet.isEmpty()) {
                    throw kasittelePoikkeus(
                        Poikkeus.TARJONTA,
                        exchange,
                        new RuntimeException("Tarjonnalta ei saatu hakukohteita haulle"),
                        Poikkeus.hakuOid(hakuOid));
                  }
                  exchange.getOut().setBody(hakukohteet);
                  dokumenttiprosessi(exchange).setKokonaistyo(hakukohteet.size());
                } catch (Exception e) {
                  LOG.error("Hakukohteiden haku epäonnistui!", e);
                  throw kasittelePoikkeus(Poikkeus.TARJONTA, exchange, e);
                }
                stopWatch.stop();
                LOG.info(stopWatch.prettyPrint());
              }
            });
  }

  protected SijoittelunTulosProsessi prosessi(Exchange exchange) {
    return exchange.getProperty(
        ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI, SijoittelunTulosProsessi.class);
  }

  private void writeLinesToTarFile(
      String fileName, byte[] data, TarArchiveOutputStream tarOutputStream) throws IOException {
    TarArchiveEntry archiveEntry = new TarArchiveEntry(fileName);
    archiveEntry.setSize(data.length);
    tarOutputStream.putArchiveEntry(archiveEntry);
    tarOutputStream.write(data);
    tarOutputStream.closeArchiveEntry();
  }

  private void writeLinesToTarFile(
      String fileName, Collection<String> lines, TarArchiveOutputStream tarOutputStream)
      throws IOException {
    TarArchiveEntry archiveEntry = new TarArchiveEntry(fileName);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    IOUtils.writeLines(lines, "\r\n", output);
    byte[] data = output.toByteArray();
    archiveEntry.setSize(data.length);
    tarOutputStream.putArchiveEntry(archiveEntry);
    tarOutputStream.write(data);
    tarOutputStream.closeArchiveEntry();
  }

  private InputStream generoiYhteenvetoTar(final Collection<Valmis> valmiit) throws IOException {
    int yhteensa = valmiit.size();
    Map<String, Collection<Valmis>> onnistuneetPerTarjoaja = Maps.newHashMap();
    Collection<Valmis> epaonnistuneet = Lists.newArrayList();
    int onnistuneita = 0;
    synchronized (valmiit) {
      for (Valmis v : valmiit) {
        if (v.isOnnistunut()) {
          ++onnistuneita;
        } else {
          epaonnistuneet.add(v);
        }
        if (onnistuneetPerTarjoaja.containsKey(v.getTarjoajaOid())) {
          onnistuneetPerTarjoaja.get(v.getTarjoajaOid()).add(v);
        } else {
          onnistuneetPerTarjoaja.put(v.getTarjoajaOid(), Lists.newArrayList(v));
        }
      }
    }
    LOG.error(
        "Sijoitteluntulosexcel valmistui! {} työtä! Joista onnistuneita {} ja epäonnistuneita {}",
        yhteensa,
        onnistuneita,
        yhteensa - onnistuneita);
    ByteArrayOutputStream tarFileBytes = new ByteArrayOutputStream();
    TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(tarFileBytes);
    {
      Collection<String> rivit = Lists.newArrayList();
      rivit.add(
          "Yhteensä "
              + yhteensa
              + ", joista onnistuneita "
              + onnistuneita
              + " ja epäonnistuneita "
              + (yhteensa - onnistuneita));

      for (Valmis epa : epaonnistuneet) {
        String otsikko;
        if (epa.isEiTuloksia()) {
          otsikko = "Tulokseton hakukohde ";
        } else {
          otsikko = "Epäonnistunut hakukohde ";
        }
        rivit.add(otsikko + epa.getHakukohdeOid());
        rivit.add("-- Tarjoaja " + epa.getTarjoajaOid());
      }
      writeLinesToTarFile("yhteenveto.txt", rivit, tarOutputStream);
    }
    for (Entry<String, Collection<Valmis>> perTarjoaja : onnistuneetPerTarjoaja.entrySet()) {
      if (perTarjoaja == null) {
        LOG.error("perTarjoaja null! Miten mahdollista!");
        continue;
      }
      String tarjoajaOid = StringUtils.trimToEmpty(perTarjoaja.getKey());
      String subFileName = "tarjoajaOid_" + tarjoajaOid.replace(" ", "_") + ".tar";

      ByteArrayOutputStream subTarFileBytes = new ByteArrayOutputStream();
      TarArchiveOutputStream subTarOutputStream = new TarArchiveOutputStream(subTarFileBytes);
      if (perTarjoaja.getValue() == null) {
        LOG.error("perTarjoaja value null! Miten mahdollista!");
        continue;
      }
      for (Valmis v : perTarjoaja.getValue()) {
        if (v.containsTiedosto()) {
          writeLinesToTarFile(
              v.getTiedosto().getTiedostonNimi(), v.getTiedosto().getData(), subTarOutputStream);
        } else if (v.isOnnistunut()) {
          String hakukohdeFileName = "hakukohdeOid_" + v.getHakukohdeOid() + ".txt";
          String kokoUrl = dokumenttipalveluUrl + v.getTulosId();
          writeLinesToTarFile(
              hakukohdeFileName, Arrays.asList(v.getTulosId(), kokoUrl), subTarOutputStream);
        }
      }
      subTarOutputStream.close();
      writeLinesToTarFile(subFileName, subTarFileBytes.toByteArray(), tarOutputStream);
    }
    tarOutputStream.close();
    return new ByteArrayInputStream(tarFileBytes.toByteArray());
  }

  private String yksittainenTyo(String type) {
    return "seda:sijoitteluntulos_"
        + type
        + "_haulle_yksittainentulos?"
        +
        // jos palvelin sammuu niin ei suorita loppuun tyojonoa
        "purgeWhenStopping=true"
        +
        // reitin kutsuja ei jaa koskaan odottamaan paluuarvoa
        "&waitForTaskToComplete=Never"
        +
        // tyojonossa on yksi tyostaja
        "&concurrentConsumers=6";
  }
}
