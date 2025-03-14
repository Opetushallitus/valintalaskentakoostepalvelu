package fi.vm.sade.valinta.kooste.valintalaskentatulos.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.AbstractHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.function.SynkronoituLaskuri;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.PoikkeusKasittelijaSovitin;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.ValintakoeKutsuExcelKomponentti;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;
import io.reactivex.Observable;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ValintakoekutsutExcelService {
  private static final Logger LOG = LoggerFactory.getLogger(ValintakoekutsutExcelService.class);

  private final ValintalaskentaValintakoeAsyncResource valintalaskentaAsyncResource;
  private final ValintaperusteetAsyncResource valintaperusteetValintakoeResource;
  private final ApplicationAsyncResource applicationResource;
  private final AtaruAsyncResource ataruAsyncResource;
  private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;
  private final TarjontaAsyncResource tarjontaAsyncResource;
  private final DokumenttiAsyncResource dokumenttiAsyncResource;
  private final ValintakoeKutsuExcelKomponentti valintakoeKutsuExcelKomponentti =
      new ValintakoeKutsuExcelKomponentti();

  @Autowired
  public ValintakoekutsutExcelService(
      ValintalaskentaValintakoeAsyncResource valintalaskentaAsyncResource,
      ValintaperusteetAsyncResource valintaperusteetValintakoeResource,
      ApplicationAsyncResource applicationResource,
      AtaruAsyncResource ataruAsyncResource,
      KoodistoCachedAsyncResource koodistoCachedAsyncResource,
      TarjontaAsyncResource tarjontaAsyncResource,
      DokumenttiAsyncResource dokumenttiAsyncResource) {
    this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;
    this.valintaperusteetValintakoeResource = valintaperusteetValintakoeResource;
    this.applicationResource = applicationResource;
    this.ataruAsyncResource = ataruAsyncResource;
    this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
    this.tarjontaAsyncResource = tarjontaAsyncResource;
    this.dokumenttiAsyncResource = dokumenttiAsyncResource;
  }

  public void luoExcel(
      DokumenttiProsessi prosessi,
      Haku haku,
      String hakukohdeOid,
      List<String> valintakoeTunnisteet,
      Set<String> hakemusOids) {
    PoikkeusKasittelijaSovitin poikkeuskasittelija =
        new PoikkeusKasittelijaSovitin(
            poikkeus -> {
              LOG.error("Valintakoekutsut excelin luonnissa tapahtui poikkeus:", poikkeus);
              prosessi
                  .getPoikkeukset()
                  .add(
                      new Poikkeus(
                          Poikkeus.KOOSTEPALVELU,
                          "Valintakoekutsut excelin luonnissa tapahtui poikkeus:",
                          poikkeus.getMessage()));
            });
    try {
      prosessi.setKokonaistyo(9); // 7 + luonti + dokumenttipalveluun vienti
      final boolean useWhitelist =
          !Optional.ofNullable(hakemusOids).orElse(Collections.emptySet()).isEmpty();
      final AtomicReference<AbstractHakukohde> hakukohdeRef = new AtomicReference<>();
      final AtomicReference<List<HakemusOsallistuminenDTO>> tiedotHakukohteelleRef =
          new AtomicReference<>();
      final AtomicReference<Map<String, ValintakoeDTO>> valintakokeetRef = new AtomicReference<>();
      final AtomicReference<List<HakemusWrapper>> haetutHakemuksetRef =
          new AtomicReference<>(Collections.emptyList());
      final Consumer<List<HakemusWrapper>> lisaaHakemuksiaAtomisestiHakemuksetReferenssiin =
          hakemuksia ->
              haetutHakemuksetRef.getAndUpdate(
                  vanhatHakemukset ->
                      Stream.concat(vanhatHakemukset.stream(), hakemuksia.stream())
                          .collect(Collectors.toList()));
      final AtomicReference<Map<String, Koodi>> maatJaValtiot1Ref = new AtomicReference<>();
      final AtomicReference<Map<String, Koodi>> postiRef = new AtomicReference<>();
      final SynkronoituLaskuri laskuri =
          SynkronoituLaskuri.builder()
              .setLaskurinAlkuarvo(7)
              .setSuoritaJokaKerta(prosessi::inkrementoiTehtyjaToita)
              .setSynkronoituToiminto(
                  () -> {
                    String hakuNimi = new Teksti(haku.nimi).getTeksti();
                    String hakukohdeNimi = new Teksti(hakukohdeRef.get().nimi).getTeksti();
                    try {
                      InputStream filedata =
                          valintakoeKutsuExcelKomponentti.luoTuloksetXlsMuodossa(
                              hakuNimi,
                              hakukohdeNimi,
                              maatJaValtiot1Ref.get(),
                              postiRef.get(),
                              tiedotHakukohteelleRef.get(),
                              valintakokeetRef.get(),
                              haetutHakemuksetRef.get().stream()
                                  .distinct()
                                  .collect(Collectors.toList()),
                              Optional.ofNullable(hakemusOids).orElse(Collections.emptySet()));
                      prosessi.inkrementoiTehtyjaToita();
                      String id = UUID.randomUUID().toString();
                      long expirationDate = DateTime.now().plusHours(168).toDate().getTime();

                      dokumenttiAsyncResource
                          .tallenna(
                              id,
                              "valintakoekutsut.xls",
                              expirationDate,
                              prosessi.getTags(),
                              "application/vnd.ms-excel",
                              filedata)
                          .subscribe(
                              ok -> {
                                prosessi.inkrementoiTehtyjaToita();
                                prosessi.setDokumenttiId(id);
                              },
                              poikkeuskasittelija);
                    } catch (Throwable t) {
                      poikkeuskasittelija.accept(t);
                    }
                  })
              .build();
      Observable.fromFuture(
              koodistoCachedAsyncResource.haeKoodistoAsync(
                  KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1))
          .subscribe(
              maatJaValtiot1 -> {
                maatJaValtiot1Ref.set(maatJaValtiot1);
                laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
              },
              poikkeuskasittelija);
      Observable.fromFuture(
              koodistoCachedAsyncResource.haeKoodistoAsync(KoodistoCachedAsyncResource.POSTI))
          .subscribe(
              posti -> {
                postiRef.set(posti);
                laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
              },
              poikkeuskasittelija);
      if (useWhitelist) {
        haeHakemuksia(
            haku,
            hakemusOids,
            poikkeuskasittelija,
            lisaaHakemuksiaAtomisestiHakemuksetReferenssiin,
            laskuri);
      }
      valintaperusteetValintakoeResource
          .haeValintakokeetHakukohteelle(hakukohdeOid)
          .subscribe(
              valintakokeet -> {
                List<ValintakoeDTO> kiinnostavatValintakokeet =
                    valintakokeet.stream()
                        .filter(v -> valintakoeTunnisteet.contains(v.getSelvitettyTunniste()))
                        .collect(Collectors.toList());
                valintakokeetRef.set(
                    kiinnostavatValintakokeet.stream()
                        .collect(Collectors.toMap(ValintakoeDTO::getSelvitettyTunniste, v -> v)));
                boolean onkoJossainValintakokeessaKaikkiHaetaan =
                    kiinnostavatValintakokeet.stream()
                        .anyMatch(vk -> Boolean.TRUE.equals(vk.getKutsutaankoKaikki()));
                // estetään ettei haeta kahteen kertaan kaikkia hakemuksia ja siten tuplata muistin
                // käyttöä. joissain hakukohteissa on tuhansia hakemuksia ja hakemusten koko voi
                // olla megatavuja.
                final SynkronoituLaskuri
                    voikoHakeaJoOsallistujienHakemuksetVaiOnkoKaikkienHakemustenHakuKesken =
                        SynkronoituLaskuri.builder()
                            .setLaskurinAlkuarvo(2)
                            .setSynkronoituToiminto(
                                () -> {
                                  if (!useWhitelist) {
                                    Set<String> osallistujienHakemusOids =
                                        Sets.newHashSet(
                                            tiedotHakukohteelleRef.get().stream()
                                                .filter(
                                                    o -> hakukohdeOid.equals(o.getHakukohdeOid()))
                                                .map(HakemusOsallistuminenDTO::getHakemusOid)
                                                .collect(Collectors.toSet()));
                                    Set<String> joHaetutHakemukset =
                                        haetutHakemuksetRef.get().stream()
                                            .map(HakemusWrapper::getOid)
                                            .collect(Collectors.toSet());
                                    osallistujienHakemusOids.removeAll(
                                        joHaetutHakemukset); // ei haeta jo haettuja hakemuksia
                                    haeHakemuksia(
                                        haku,
                                        osallistujienHakemusOids,
                                        poikkeuskasittelija,
                                        lisaaHakemuksiaAtomisestiHakemuksetReferenssiin,
                                        laskuri);
                                  }
                                  laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                                })
                            .build();

                if (onkoJossainValintakokeessaKaikkiHaetaan && !useWhitelist) {
                  if (haku.isHakemuspalvelu()) {
                    Observable.fromFuture(
                            ataruAsyncResource.getApplicationsByHakukohde(hakukohdeOid))
                        .subscribe(
                            hakemukset -> {
                              lisaaHakemuksiaAtomisestiHakemuksetReferenssiin.accept(hakemukset);
                              laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                              voikoHakeaJoOsallistujienHakemuksetVaiOnkoKaikkienHakemustenHakuKesken
                                  .vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                            },
                            poikkeuskasittelija);
                  } else {
                    Observable.fromFuture(
                            applicationResource.getApplicationsByOid(haku.oid, hakukohdeOid))
                        .subscribe(
                            hakemukset -> {
                              lisaaHakemuksiaAtomisestiHakemuksetReferenssiin.accept(hakemukset);
                              laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                              voikoHakeaJoOsallistujienHakemuksetVaiOnkoKaikkienHakemustenHakuKesken
                                  .vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                            },
                            poikkeuskasittelija);
                  }

                } else {
                  laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                  voikoHakeaJoOsallistujienHakemuksetVaiOnkoKaikkienHakemustenHakuKesken
                      .vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                }
                Observable.fromFuture(
                        valintalaskentaAsyncResource.haeValintatiedotHakukohteelle(
                            hakukohdeOid,
                            kiinnostavatValintakokeet.stream()
                                .map(ValintakoeDTO::getSelvitettyTunniste)
                                .collect(Collectors.toList())))
                    .subscribe(
                        osallistuminen -> {
                          List<HakemusOsallistuminenDTO> hakukohteeseenOsallistujat =
                              osallistuminen.stream()
                                  .filter(o -> hakukohdeOid.equals(o.getHakukohdeOid()))
                                  .collect(Collectors.toList());
                          tiedotHakukohteelleRef.set(hakukohteeseenOsallistujat);
                          voikoHakeaJoOsallistujienHakemuksetVaiOnkoKaikkienHakemustenHakuKesken
                              .vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                        },
                        poikkeuskasittelija);
                laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
              },
              poikkeuskasittelija);
      Observable.fromFuture(tarjontaAsyncResource.haeHakukohde(hakukohdeOid))
          .subscribe(
              hakukohde -> {
                hakukohdeRef.set(hakukohde);
                laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
              },
              poikkeuskasittelija);
    } catch (Throwable t) {
      poikkeuskasittelija.accept(t);
    }
  }

  private void haeHakemuksia(
      Haku haku,
      Set<String> hakemusOids,
      PoikkeusKasittelijaSovitin poikkeuskasittelija,
      Consumer<List<HakemusWrapper>> lisaaHakemuksiaAtomisestiHakemuksetReferenssiin,
      SynkronoituLaskuri laskuri) {
    if (haku.isHakemuspalvelu()) {
      Observable.fromFuture(
              ataruAsyncResource.getApplicationsByOids(Lists.newArrayList(hakemusOids)))
          .subscribe(
              hakemukset -> {
                lisaaHakemuksiaAtomisestiHakemuksetReferenssiin.accept(hakemukset);
                laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
              },
              poikkeuskasittelija);
    } else {
      applicationResource
          .getApplicationsByOids(hakemusOids)
          .subscribe(
              hakemukset -> {
                lisaaHakemuksiaAtomisestiHakemuksetReferenssiin.accept(hakemukset);
                laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
              },
              poikkeuskasittelija);
    }
  }
}
