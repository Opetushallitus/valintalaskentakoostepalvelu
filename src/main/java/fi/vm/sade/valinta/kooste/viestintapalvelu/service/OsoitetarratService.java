package fi.vm.sade.valinta.kooste.viestintapalvelu.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fi.vm.sade.sijoittelu.tulos.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ataru.IsAtaruHakemusOid;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.function.SynkronoituLaskuri;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.NimiPaattelyStrategy;
import fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.util.PoikkeusKasittelijaSovitin;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen;
import io.reactivex.Observable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.poi.util.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OsoitetarratService {

  private static final Logger LOG = LoggerFactory.getLogger(OsoitetarratService.class);
  private final ApplicationAsyncResource applicationAsyncResource;
  private final AtaruAsyncResource ataruAsyncResource;
  private final HaeOsoiteKomponentti osoiteKomponentti;
  private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;
  private final DokumenttiAsyncResource dokumenttiAsyncResource;
  private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
  private final ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource;
  private final ValintaperusteetAsyncResource valintaperusteetValintakoeResource;
  private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;

  @Autowired
  public OsoitetarratService(
      ApplicationAsyncResource applicationAsyncResource,
      AtaruAsyncResource ataruAsyncResource,
      KoodistoCachedAsyncResource koodistoCachedAsyncResource,
      DokumenttiAsyncResource dokumenttiAsyncResource,
      ViestintapalveluAsyncResource viestintapalveluAsyncResource,
      ValintaperusteetAsyncResource valintaperusteetValintakoeResource,
      ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource,
      ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource,
      HaeOsoiteKomponentti osoiteKomponentti) {
    this.applicationAsyncResource = applicationAsyncResource;
    this.ataruAsyncResource = ataruAsyncResource;
    this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
    this.dokumenttiAsyncResource = dokumenttiAsyncResource;
    this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
    this.valintaperusteetValintakoeResource = valintaperusteetValintakoeResource;
    this.valintalaskentaValintakoeAsyncResource = valintalaskentaValintakoeAsyncResource;
    this.valintaTulosServiceAsyncResource = valintaTulosServiceAsyncResource;
    this.osoiteKomponentti = osoiteKomponentti;
  }

  public void osoitetarratSijoittelussaHyvaksytyille(
      DokumenttiProsessi prosessi, Haku haku, String hakukohdeOid) {
    Consumer<Throwable> poikkeuskasittelija = poikkeuskasittelija(prosessi);
    try {
      LOG.error(
          "Luodaan osoitetarrat sijoittelussa hyväksytyille (haku={}, hakukohde={})",
          haku.oid,
          hakukohdeOid);
      prosessi.setKokonaistyo(5);
      final AtomicReference<List<HakemusWrapper>> haetutHakemuksetRef = new AtomicReference<>();
      final AtomicReference<Map<String, Koodi>> maatJaValtiot1Ref = new AtomicReference<>();
      final AtomicReference<Map<String, Koodi>> postiRef = new AtomicReference<>();
      final SynkronoituLaskuri laskuri =
          SynkronoituLaskuri.builder()
              .setLaskurinAlkuarvo(3)
              .setSuoritaJokaKerta(prosessi::inkrementoiTehtyjaToita)
              .setSynkronoituToiminto(
                  () -> {
                    osoitetarratHakemuksille(
                        haetutHakemuksetRef.get(),
                        maatJaValtiot1Ref.get(),
                        postiRef.get(),
                        prosessi);
                  })
              .build();
      maatJaValtiot1(laskuri, maatJaValtiot1Ref, poikkeuskasittelija);
      posti(laskuri, postiRef, poikkeuskasittelija);

      valintaTulosServiceAsyncResource
          .getHakukohdeBySijoitteluajoPlainDTO(haku.oid, hakukohdeOid)
          .map(
              hakukohteenTulos ->
                  hakukohteenTulos.getValintatapajonot().stream()
                      .flatMap(valintatapajono -> valintatapajono.getHakemukset().stream())
                      .filter(hakemus -> hakemus.getTila().isHyvaksytty())
                      .map(HakemusDTO::getHakemusOid)
                      .distinct()
                      .collect(Collectors.toList()))
          .flatMap(
              hakemusOids -> {
                if (hakemusOids.isEmpty()) {
                  return Observable.error(
                      new RuntimeException("Sijoittelussa ei ole hyväksyttyjä hakijoita"));
                } else {
                  return haku.isHakemuspalvelu()
                      ? Observable.fromFuture(ataruAsyncResource.getApplicationsByOids(hakemusOids))
                      : applicationAsyncResource.getApplicationsByOids(hakemusOids);
                }
              })
          .subscribe(
              hakemukset -> {
                haetutHakemuksetRef.set(hakemukset);
                laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
              },
              poikkeuskasittelija::accept);
    } catch (Throwable t) {
      poikkeuskasittelija.accept(t);
    }
  }

  public void osoitetarratValintakokeeseenOsallistujille(
      DokumenttiProsessi prosessi,
      Haku haku,
      String hakukohdeOid,
      Set<String> selvitetytTunnisteet) {
    PoikkeusKasittelijaSovitin poikkeuskasittelija = poikkeuskasittelija(prosessi);
    try {
      LOG.info(
          "Luodaan osoitetarrat valintakokeeseen osallistujille (haku={}, hakukohde={})",
          haku.oid,
          hakukohdeOid);
      prosessi.setKokonaistyo(5 + 1 + 1); // AtomicReferencet + luonti + dokumenttipalveluun vienti
      final AtomicReference<List<ValintakoeOsallistuminenDTO>> osallistumistiedotRef =
          new AtomicReference<>();
      final AtomicReference<List<HakemusWrapper>> haetutHakemuksetRef = new AtomicReference<>();
      final AtomicReference<Map<String, Koodi>> maatJaValtiot1Ref = new AtomicReference<>();
      final AtomicReference<Map<String, Koodi>> postiRef = new AtomicReference<>();
      final SynkronoituLaskuri laskuri =
          SynkronoituLaskuri.builder()
              .setLaskurinAlkuarvo(2 + 1) // maat ja valtiot + posti + ulkopuoliset hakijat
              .setSuoritaJokaKerta(prosessi::inkrementoiTehtyjaToita)
              .setSynkronoituToiminto(
                  () ->
                      osoitetarratHakemuksille(
                          haetutHakemuksetRef.get(),
                          maatJaValtiot1Ref.get(),
                          postiRef.get(),
                          prosessi))
              .build();
      final SynkronoituLaskuri laskuriHakukohteenUlkopuolisilleHakijoille =
          SynkronoituLaskuri.builder()
              .setLaskurinAlkuarvo(
                  1 + 1) // Kaikki hakijat jos kaikki kutsutaan + Osallistumistiedot mistä selviää
              // hakukohteen ulkopuoliset hakijat
              .setSuoritaJokaKerta(prosessi::inkrementoiTehtyjaToita)
              .setSynkronoituToiminto(
                  () -> {
                    // Haetaan ulkopuoliset hakijat
                    List<ValintakoeOsallistuminenDTO> osallistumistiedot =
                        osallistumistiedotRef.get();
                    Set<String> valintakokeisiinOsallistujienHakemusOidit =
                        osallistumistiedot.stream()
                            .filter(
                                o ->
                                    o.getHakutoiveet().stream()
                                        .anyMatch(
                                            h ->
                                                hakukohdeOid.equals(h.getHakukohdeOid())
                                                    && h.getValinnanVaiheet().stream()
                                                        .anyMatch(
                                                            v ->
                                                                v.getValintakokeet().stream()
                                                                    .anyMatch(
                                                                        vk ->
                                                                            selvitetytTunnisteet
                                                                                    .contains(
                                                                                        vk
                                                                                            .getValintakoeTunniste())
                                                                                && Osallistuminen
                                                                                    .OSALLISTUU
                                                                                    .equals(
                                                                                        vk.getOsallistuminenTulos()
                                                                                            .getOsallistuminen())))))
                            .map(ValintakoeOsallistuminenDTO::getHakemusOid)
                            .collect(Collectors.toSet());
                    Set<String> mahdollisestiHakukohteenHakemusOidit =
                        haetutHakemuksetRef.get().stream()
                            .map(HakemusWrapper::getOid)
                            .collect(Collectors.toSet());
                    if (mahdollisestiHakukohteenHakemusOidit.containsAll(
                        valintakokeisiinOsallistujienHakemusOidit)) {
                      laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                    } else {
                      Set<String> puuttuvatHakemusOidit =
                          Sets.newHashSet(valintakokeisiinOsallistujienHakemusOidit);
                      puuttuvatHakemusOidit.removeAll(mahdollisestiHakukohteenHakemusOidit);
                      (haku.isHakemuspalvelu()
                              ? Observable.fromFuture(
                                  ataruAsyncResource.getApplicationsByOids(
                                      Lists.newArrayList(puuttuvatHakemusOidit)))
                              : applicationAsyncResource.getApplicationsByOids(
                                  puuttuvatHakemusOidit))
                          .subscribe(
                              puuttuvatHakemukset -> {
                                haetutHakemuksetRef.set(
                                    Stream.concat(
                                            haetutHakemuksetRef.get().stream(),
                                            puuttuvatHakemukset.stream())
                                        .collect(Collectors.toList()));
                                laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                              },
                              poikkeuskasittelija);
                    }
                  })
              .build();
      maatJaValtiot1(laskuri, maatJaValtiot1Ref, poikkeuskasittelija);
      posti(laskuri, postiRef, poikkeuskasittelija);

      valintaperusteetValintakoeResource
          .haeValintakokeetHakukohteelle(hakukohdeOid)
          .subscribe(
              valintakokeet -> {
                boolean kutsutaankoJossainKokeessaKaikki =
                    valintakokeet.stream()
                        .anyMatch(
                            vk ->
                                selvitetytTunnisteet.contains(vk.getSelvitettyTunniste())
                                    && Boolean.TRUE.equals(vk.getKutsutaankoKaikki()));
                if (kutsutaankoJossainKokeessaKaikki) {
                  (haku.isHakemuspalvelu()
                          ? Observable.fromFuture(
                              ataruAsyncResource.getApplicationsByHakukohde(hakukohdeOid))
                          : Observable.fromFuture(
                              applicationAsyncResource.getApplicationsByOid(
                                  haku.oid, hakukohdeOid)))
                      .subscribe(
                          hakemukset -> {
                            haetutHakemuksetRef.set(hakemukset);
                            laskuriHakukohteenUlkopuolisilleHakijoille
                                .vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                          },
                          poikkeuskasittelija);
                } else {
                  haetutHakemuksetRef.set(Collections.emptyList());
                  laskuriHakukohteenUlkopuolisilleHakijoille
                      .vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                }
              },
              poikkeuskasittelija);
      Observable.fromFuture(valintalaskentaValintakoeAsyncResource.haeHakutoiveelle(hakukohdeOid))
          .subscribe(
              osallistumiset -> {
                osallistumistiedotRef.set(osallistumiset);
                laskuriHakukohteenUlkopuolisilleHakijoille
                    .vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
              },
              poikkeuskasittelija);
    } catch (Throwable t) {
      poikkeuskasittelija.accept(t);
    }
  }

  public void osoitetarratHakemuksille(DokumenttiProsessi prosessi, List<String> hakemusOids) {
    Consumer<Throwable> poikkeuskasittelija = poikkeuskasittelija(prosessi);
    try {
      prosessi.setKokonaistyo(5);
      final AtomicReference<List<HakemusWrapper>> haetutHakemuksetRef = new AtomicReference<>();
      final AtomicReference<Map<String, Koodi>> maatJaValtiot1Ref = new AtomicReference<>();
      final AtomicReference<Map<String, Koodi>> postiRef = new AtomicReference<>();
      final SynkronoituLaskuri laskuri =
          SynkronoituLaskuri.builder()
              .setLaskurinAlkuarvo(3)
              .setSuoritaJokaKerta(prosessi::inkrementoiTehtyjaToita)
              .setSynkronoituToiminto(
                  () -> {
                    osoitetarratHakemuksille(
                        haetutHakemuksetRef.get(),
                        maatJaValtiot1Ref.get(),
                        postiRef.get(),
                        prosessi);
                  })
              .build();
      maatJaValtiot1(laskuri, maatJaValtiot1Ref, poikkeuskasittelija);
      posti(laskuri, postiRef, poikkeuskasittelija);

      hakemusOids.stream()
        .findFirst()
        .filter(IsAtaruHakemusOid::isAtaruHakemusOid)
        .map(any -> Observable.fromFuture(ataruAsyncResource.getApplicationsByOids(hakemusOids)))
        .orElseGet(() -> applicationAsyncResource
          .getApplicationsByHakemusOids(hakemusOids))
          .subscribe(
              hakemukset -> {
                haetutHakemuksetRef.set(hakemukset);
                laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
              },
              poikkeuskasittelija::accept);
    } catch (Throwable t) {
      poikkeuskasittelija.accept(t);
    }
  }

  private void osoitetarratHakemuksille(
      List<HakemusWrapper> haetutHakemukset,
      Map<String, Koodi> maatJaValtiot1,
      Map<String, Koodi> posti,
      DokumenttiProsessi prosessi) {
    Consumer<Throwable> poikkeuskasittelija = poikkeuskasittelija(prosessi);
    try {
      Osoitteet osoitteet =
          new Osoitteet(
              haetutHakemukset.stream()
                  .map(
                      h ->
                          OsoiteHakemukseltaUtil.osoiteHakemuksesta(
                              h, maatJaValtiot1, posti, new NimiPaattelyStrategy()))
                  .collect(Collectors.toList()));
      // Aakkosjarjestykseen
      osoitteet
          .getAddressLabels()
          .sort(
              (o1, o2) -> {
                int i =
                    Optional.ofNullable(o1.getFirstName())
                        .orElse("")
                        .compareTo(Optional.ofNullable(o2.getFirstName()).orElse(""));
                if (i == 0) {
                  return Optional.ofNullable(o1.getLastName())
                      .orElse("")
                      .compareTo(Optional.ofNullable(o2.getLastName()).orElse(""));
                } else {
                  return i;
                }
              });
      // LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(osoitteet));
      viestintapalveluAsyncResource
          .haeOsoitetarrat(osoitteet)
          .subscribe(
              response -> {
                prosessi.inkrementoiTehtyjaToita();
                String id = UUID.randomUUID().toString();
                try {
                  InputStream inputStream = pipeInputStreams((InputStream) response.getEntity());
                  dokumenttiAsyncResource
                      .tallenna(
                          id,
                          "osoitetarrat.pdf",
                          defaultExpirationDate().getTime(),
                          prosessi.getTags(),
                          "application/pdf",
                          inputStream)
                      .subscribe(
                          ok -> {
                            prosessi.inkrementoiTehtyjaToita();
                            prosessi.setDokumenttiId(id);
                          },
                          poikkeus -> {
                            LOG.error(
                                "Osoitetarrojen luonti epäonnistui dokumentin tallennukseen:",
                                poikkeus);
                            prosessi
                                .getPoikkeukset()
                                .add(
                                    new Poikkeus(
                                        Poikkeus.KOOSTEPALVELU,
                                        "Osoitetarrojen tallennus epäonnistui:",
                                        poikkeus.getMessage()));
                          });
                } catch (Throwable t) {
                  poikkeuskasittelija.accept(t);
                }
              },
              poikkeus -> {
                LOG.error("Osoitetarrojen luonti epäonnistui viestintäpalvelukutsuun:", poikkeus);
                prosessi
                    .getPoikkeukset()
                    .add(
                        new Poikkeus(
                            Poikkeus.KOOSTEPALVELU,
                            "Osoitetarrojen luonti viestintäpalvelussa epäonnistui:",
                            poikkeus.getMessage()));
              });
    } catch (Throwable t) {
      poikkeuskasittelija.accept(t);
    }
  }

  private Date defaultExpirationDate() {
    return DateTime.now().plusHours(168).toDate(); // almost a day
  }

  private PoikkeusKasittelijaSovitin poikkeuskasittelija(DokumenttiProsessi prosessi) {
    return new PoikkeusKasittelijaSovitin(
        poikkeus -> {
          LOG.error("Osoitetarrojen luonti epäonnistui:", poikkeus);
          prosessi
              .getPoikkeukset()
              .add(
                  new Poikkeus(
                      Poikkeus.KOOSTEPALVELU,
                      "Osoitetarrojen luonti epäonnistui:",
                      poikkeus.getMessage()));
        });
  }

  private void maatJaValtiot1(
      final SynkronoituLaskuri laskuri,
      AtomicReference<Map<String, Koodi>> maatJaValtiot1Ref,
      Consumer<Throwable> poikkeuskasittelija) {
    Observable.fromFuture(
            koodistoCachedAsyncResource.haeKoodistoAsync(
                KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1))
        .subscribe(
            maatJaValtiot1 -> {
              maatJaValtiot1Ref.set(maatJaValtiot1);
              laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
            },
            poikkeuskasittelija::accept);
  }

  private void posti(
      final SynkronoituLaskuri laskuri,
      AtomicReference<Map<String, Koodi>> postiRef,
      Consumer<Throwable> poikkeuskasittelija) {
    Observable.fromFuture(
            koodistoCachedAsyncResource.haeKoodistoAsync(KoodistoCachedAsyncResource.POSTI))
        .subscribe(
            posti -> {
              postiRef.set(posti);
              laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
            },
            poikkeuskasittelija::accept);
  }

  private InputStream pipeInputStreams(InputStream incoming) throws IOException {
    byte[] dokumentti = IOUtils.toByteArray(incoming);
    if (dokumentti == null || dokumentti.length == 0) {
      throw new RuntimeException("Viestintäpalvelu palautti tyhjän dokumentin!");
    }
    InputStream p = new ByteArrayInputStream(dokumentti);
    incoming.close();
    return p;
  }
}
