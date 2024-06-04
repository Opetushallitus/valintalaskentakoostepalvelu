package fi.vm.sade.valinta.kooste.sijoitteluntulos.route.impl;

import static fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.DokumenttiUtils.defaultExpirationDate;
import static fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.DokumenttiUtils.generateId;
import static java.util.concurrent.TimeUnit.MINUTES;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
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
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.Tiedosto;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.Valmis;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosOsoitetarratRoute;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosTaulukkolaskentaRoute;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.util.NimiPaattelyStrategy;
import fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Varoitus;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.SijoittelultaEiSisaltoaPoikkeus;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.SijoittelunTulosExcelKomponentti;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.valvomo.dto.Tunniste;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Component
public class SijoittelunTulosRouteImpl
    implements SijoittelunTulosTaulukkolaskentaRoute, SijoittelunTulosOsoitetarratRoute {
  private static final Logger LOG = LoggerFactory.getLogger(SijoittelunTulosRouteImpl.class);

  private final long getTimeToLive() {
    return DateTime.now().plusHours(720).toDate().getTime();
  }

  private final boolean pakkaaTiedostotTarriin;
  private final SijoittelunTulosExcelKomponentti sijoittelunTulosExcel;
  private final DokumenttiAsyncResource dokumenttiAsyncResource;
  private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
  private final ApplicationAsyncResource applicationAsyncResource;
  private final String dokumenttipalveluUrl;
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
      KoodistoCachedAsyncResource koodistoCachedAsyncResource,
      SijoittelunTulosExcelKomponentti sijoittelunTulosExcel,
      TarjontaAsyncResource tarjontaAsyncResource,
      OrganisaatioAsyncResource organisaatioAsyncResource,
      ViestintapalveluAsyncResource viestintapalveluAsyncResource,
      ApplicationAsyncResource applicationAsyncResource,
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
    this.applicationAsyncResource = applicationAsyncResource;
    this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
    this.dokumenttipalveluUrl = dokumenttipalveluUrl;
    this.sijoittelunTulosExcel = sijoittelunTulosExcel;
    this.dokumenttiAsyncResource = dokumenttiAsyncResource;
    this.valintalaskentaResource = valintalaskentaResource;
  }

  private void taulukkolaskenta(AuditSession auditSession, SijoittelunTulosProsessi prosessi) {
    Set<String> hakukohdeOids = hakukohteidenHaku(prosessi);

    for (String hakukohdeOid : hakukohdeOids) {
      LOG.info("configureTaulukkolaskenta hakukohdeOid: " + hakukohdeOid);
      String hakuOid = prosessi.getHakuOid();
      String tarjoajaOid = StringUtils.EMPTY;
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
        hakuOid = hakukohde.hakuOid;
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
                        applicationAsyncResource
                            .getApplicationsByOids(hakuOid, Collections.singleton(hakukohdeOid))
                            .get()))
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
        valinnanvaiheet = valintalaskentaResource.laskennantulokset(hakukohdeOid).get(1, MINUTES);
      } catch (Exception e) {
        LOG.error("Problem when getting resources for creating the excel", e);
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
                    Stream.concat(prosessi.getTags().stream(), Stream.of(hakukohdeOid)).toList(),
                    "application/vnd.ms-excel",
                    input)
                .subscribe(
                    ok -> {
                      prosessi.getValmiit().add(new Valmis(hakukohdeOid, finalTarjoajaOid, id));
                    },
                    poikkeus -> {
                      LOG.error(
                          "Sijoittelun tulosexcelin tallennus dokumenttipalveluun epäonnistui");
                      throw new RuntimeException(poikkeus);
                    });
          } catch (Exception e) {
            LOG.error("Dokumentin tallennus epäonnistui hakukohteelle " + hakukohdeOid, e);
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
        LOG.error("Sijoitteluntulosexcelin luonti epäonnistui hakukohteelle " + hakukohdeOid, e);
        prosessi
            .getVaroitukset()
            .add(
                new Varoitus(
                    hakukohdeOid,
                    "Ei saatu sijoittelun tuloksia tai hakukohteita! " + e.getMessage()));
        prosessi.getValmiit().add(new Valmis(hakukohdeOid, tarjoajaOid, null));
      }
      muodostaDokumentit(prosessi);
    }
  }

  protected InputStream pipeInputStreams(InputStream incoming) throws IOException {
    byte[] dokumentti = org.apache.poi.util.IOUtils.toByteArray(incoming);
    if (dokumentti == null || dokumentti.length == 0) {
      throw new RuntimeException("Viestintäpalvelu palautti tyhjän dokumentin!");
    }
    InputStream p = new ByteArrayInputStream(dokumentti);
    incoming.close();
    return p;
  }

  private void osoitetarrat(SijoittelunTulosProsessi prosessi) {
    Set<String> hakukohdeOids = hakukohteidenHaku(prosessi);

    /*
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
    */
    for (String hakukohdeOid : hakukohdeOids) {
      String hakuOid = prosessi.getHakuOid();
      LOG.info("configureOsoitetarrat hakukohdeOid: " + hakukohdeOid);
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
            koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
        Map<String, Koodi> posti =
            koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI);
        stopWatch.stop();
        stopWatch.start("Tiedot valintarekisteristä");
        fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO hakukohteenTulos =
            valintaTulosServiceAsyncResource
                .getHakukohdeBySijoitteluajoPlainDTO(hakuOid, hakukohdeOid)
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
            Observable.fromFuture(tarjontaAsyncResource.haeHaku(hakuOid))
                .flatMap(
                    haku ->
                        haku.isHakemuspalvelu()
                            ? Observable.fromFuture(
                                ataruAsyncResource.getApplicationsByOids(hyvaksytytHakemukset))
                            : applicationAsyncResource.getApplicationsByOids(hyvaksytytHakemukset))
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
                      viestintapalveluAsyncResource.haeOsoitetarrat(osoitteet).blockingFirst()));
          prosessi.getValmiit().add(new Valmis(tiedosto, hakukohdeOid, tarjoajaOid));
          return;
        } else {
          InputStream input =
              pipeInputStreams(
                  viestintapalveluAsyncResource.haeOsoitetarrat(osoitteet).blockingFirst());
          String id = generateId();
          String finalTarjoajaOid = tarjoajaOid;
          dokumenttiAsyncResource
              .tallenna(
                  id,
                  "osoitetarrat_" + hakukohdeOid + ".pdf",
                  getTimeToLive(),
                  Stream.concat(prosessi.getTags().stream(), Stream.of(hakukohdeOid)).toList(),
                  "application/pdf",
                  input)
              .subscribe(
                  ok -> {
                    prosessi.getValmiit().add(new Valmis(hakukohdeOid, finalTarjoajaOid, id));
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
    muodostaDokumentit(prosessi);
  }

  private void muodostaDokumentit(SijoittelunTulosProsessi prosessi) {
    // from(muodostaDokumentit) direct:sijoitteluntulos_muodosta_dokumentit
    if (prosessi.inkrementoi() == 0) {
      try {
        InputStream tar = generoiYhteenvetoTar(prosessi.getValmiit());
        String id = generateId();
        dokumenttiAsyncResource
            .tallenna(
                id,
                "sijoitteluntuloksethaulle.tar",
                defaultExpirationDate().getTime(),
                prosessi.getTags(),
                "application/x-tar",
                tar)
            .subscribe(
                ok -> {
                  prosessi.setDokumenttiId(id);
                },
                poikkeus -> {
                  LOG.error("Sijoittelun tulostietojen tallennus dokumenttipalveluun epäonnistui");
                  throw new RuntimeException(poikkeus);
                });
      } catch (Exception e) {
        LOG.error("Tulostietojen tallennus dokumenttipalveluun epäonnistui!", e);
        prosessi
            .getPoikkeukset()
            .add(new Poikkeus(Poikkeus.DOKUMENTTIPALVELU, "Tulostietojen tallennus epäonnistui!"));
      }
    }
  }

  protected RuntimeException kasittelePoikkeus(
      String palvelu, DokumenttiProsessi prosessi, Exception exception, Tunniste... oids) {
    prosessi
        .getPoikkeukset()
        .add(new Poikkeus(palvelu, StringUtils.EMPTY, exception.getMessage(), oids));
    return new RuntimeException(exception);
  }

  private Set<String> hakukohteidenHaku(DokumenttiProsessi prosessi) {
    String hakuOid = prosessi.getHakuOid();
    StopWatch stopWatch = new StopWatch("Haun " + hakuOid + " hakukohteiden haku tarjonnasta");
    stopWatch.start();
    try {
      prosessi
          .getVaroitukset()
          .add(
              new Varoitus(
                  hakuOid,
                  "Haetaan tarjonnalta kaikki hakukohteet! Varoitus, pyyntö saattaa kestää pitkään!"));
      Set<String> hakukohdeOids =
          tarjontaAsyncResource.haunHakukohteet(hakuOid).get(1, TimeUnit.MINUTES);
      if (hakukohdeOids == null || hakukohdeOids.isEmpty()) {
        throw kasittelePoikkeus(
            Poikkeus.TARJONTA,
            prosessi,
            new RuntimeException("Tarjonnalta ei saatu hakukohteita haulle"),
            Poikkeus.hakuOid(hakuOid));
      } else {
        LOG.info(
            "configureHakukohteidenHaku : saatiin tarjonnalta {} hakukohdeOidia haulle {}",
            hakukohdeOids.size(),
            hakuOid);
      }
      prosessi.setKokonaistyo(hakukohdeOids.size());
      return hakukohdeOids;
    } catch (Exception e) {
      LOG.error("Hakukohteiden haku epäonnistui!", e);
      throw kasittelePoikkeus(Poikkeus.TARJONTA, prosessi, e);
    } finally {
      stopWatch.stop();
      LOG.info(stopWatch.prettyPrint());
    }
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

  @Override
  public void taulukkolaskennatHaulle(
      SijoittelunTulosProsessi prosessi,
      String hakuOid,
      String sijoitteluAjoId,
      AuditSession session,
      Authentication auth) {
    taulukkolaskenta(session, prosessi);
  }

  @Override
  public void osoitetarratHaulle(
      SijoittelunTulosProsessi prosessi,
      String hakuOid,
      String sijoitteluAjoId,
      Authentication auth) {
    osoitetarrat(prosessi);
  }
}
