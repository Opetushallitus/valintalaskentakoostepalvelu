package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import static io.reactivex.Observable.zip;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuExcel;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRiviBuilder;
import fi.vm.sade.valinta.kooste.erillishaku.excel.Sukupuoli;
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
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Lukuvuosimaksu;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Maksuntila;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Valinnantulos;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ErillishaunVientiService {
  private static final Logger LOG = LoggerFactory.getLogger(ErillishaunVientiService.class);
  private final ValintaTulosServiceAsyncResource tilaAsyncResource;
  private final TarjontaAsyncResource hakuV1AsyncResource;
  private final ApplicationAsyncResource applicationAsyncResource;
  private final AtaruAsyncResource ataruAsyncResource;
  private final DokumenttiAsyncResource dokumenttiAsyncResource;
  private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;
  private final OrganisaatioAsyncResource organisaatioAsyncResource;

  @Autowired
  public ErillishaunVientiService(
      ValintaTulosServiceAsyncResource tilaAsyncResource,
      ApplicationAsyncResource applicationAsyncResource,
      AtaruAsyncResource ataruAsyncResource,
      TarjontaAsyncResource hakuV1AsyncResource,
      DokumenttiAsyncResource dokumenttiAsyncResource,
      KoodistoCachedAsyncResource koodistoCachedAsyncResource,
      OrganisaatioAsyncResource organisaatioAsyncResource) {
    this.tilaAsyncResource = tilaAsyncResource;
    this.hakuV1AsyncResource = hakuV1AsyncResource;
    this.applicationAsyncResource = applicationAsyncResource;
    this.ataruAsyncResource = ataruAsyncResource;
    this.dokumenttiAsyncResource = dokumenttiAsyncResource;
    this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
    this.organisaatioAsyncResource = organisaatioAsyncResource;
    LOG.info("Luetaan valinnantulokset ja sijoittelu valinta-tulos-servicestä!!!!!!!");
  }

  public void vie(
      final AuditSession auditSession, KirjeProsessi prosessi, ErillishakuDTO erillishaku) {
    Observable<Haku> hakuFuture =
        Observable.fromFuture(hakuV1AsyncResource.haeHaku(erillishaku.getHakuOid()));
    Observable<List<HakemusWrapper>> hakemusObservable =
        hakuFuture.flatMap(
            haku -> {
              if (haku.isHakemuspalvelu()) {
                return Observable.fromFuture(
                    ataruAsyncResource.getApplicationsByHakukohde(erillishaku.getHakukohdeOid()));
              } else {
                return Observable.fromFuture(
                    applicationAsyncResource.getApplicationsByOid(
                        erillishaku.getHakuOid(), erillishaku.getHakukohdeOid()));
              }
            });
    Observable<AbstractHakukohde> tarjontaHakukohdeObservable =
        Observable.fromFuture(hakuV1AsyncResource.haeHakukohde(erillishaku.getHakukohdeOid()));
    Observable<List<Organisaatio>> tarjoajatObservable =
        tarjontaHakukohdeObservable.flatMap(
            hakukohde ->
                Observable.fromFuture(
                    CompletableFutureUtil.sequence(
                        hakukohde.tarjoajaOids.stream()
                            .map(organisaatioAsyncResource::haeOrganisaatio)
                            .collect(Collectors.toList()))));
    Observable<List<Lukuvuosimaksu>> lukuvuosimaksutObs =
        tilaAsyncResource.fetchLukuvuosimaksut(erillishaku.getHakukohdeOid(), auditSession);

    Observable<ErillishakuExcel> erillishakuExcel =
        generoiValintarekisterista(
            auditSession,
            erillishaku,
            hakemusObservable,
            hakuFuture,
            tarjontaHakukohdeObservable,
            tarjoajatObservable,
            lukuvuosimaksutObs);

    erillishakuExcel
        .subscribeOn(Schedulers.newThread())
        .subscribe(
            excel -> {
              LOG.info("Aloitetaan dokumenttipalveluun tallennus");
              String uuid = UUID.randomUUID().toString();
              dokumenttiAsyncResource
                  .tallenna(
                      uuid,
                      "erillishaku.xlsx",
                      DateTime.now().plusHours(1).toDate().getTime(),
                      List.of("erillishaku"),
                      "application/octet-stream",
                      excel.getExcel().vieXlsx())
                  .subscribe(
                      ok -> {
                        LOG.info("Erillishaun vienti onnistui!");
                        prosessi.vaiheValmistui();
                        prosessi.valmistui(uuid);
                        prosessi.keskeyta();
                      },
                      poikkeus -> {
                        LOG.error("Erillihakuexcelin tallennus dokumenttipalveluun epäonnistui");
                        throw new RuntimeException(poikkeus);
                      });
            },
            poikkeus -> {
              LOG.error("Erillishaun vienti keskeytyi virheeseen", poikkeus);
              prosessi.keskeyta();
            });
  }

  private String objectToString(Object o) {
    if (o == null) {
      return "";
    } else {
      return o.toString();
    }
  }

  private Observable<ErillishakuExcel> generoiValintarekisterista(
      AuditSession auditSession,
      ErillishakuDTO erillishaku,
      Observable<List<HakemusWrapper>> hakemusObservable,
      Observable<Haku> hakuFuture,
      Observable<AbstractHakukohde> tarjontaHakukohdeObservable,
      Observable<List<Organisaatio>> tarjoajatObservable,
      Observable<List<Lukuvuosimaksu>> lukuvuosimaksuObs) {
    Observable<List<Valinnantulos>> valinnantulosObservable =
        tilaAsyncResource.getErillishaunValinnantulokset(
            auditSession, erillishaku.getValintatapajonoOid());

    return zip(
        hakemusObservable,
        hakuFuture,
        tarjontaHakukohdeObservable,
        tarjoajatObservable,
        valinnantulosObservable,
        lukuvuosimaksuObs,
        (hakemukset, haku, tarjontaHakukohde, tarjoajat, valinnantulos, lukuvuosimaksus) -> {
          if (valinnantulos.isEmpty()) {
            return generoiIlmanHakukohdettaJaTuloksia(
                erillishaku, hakemukset, lukuvuosimaksus, haku, tarjontaHakukohde, tarjoajat);
          } else {
            return generoiValinnantuloksista(
                erillishaku,
                hakemukset,
                lukuvuosimaksus,
                haku,
                tarjontaHakukohde,
                tarjoajat,
                valinnantulos);
          }
        });
  }

  private ErillishakuExcel generoiValinnantuloksista(
      final ErillishakuDTO erillishaku,
      final List<HakemusWrapper> hakemukset,
      final List<Lukuvuosimaksu> lukuvuosimaksus,
      final Haku haku,
      final AbstractHakukohde tarjontaHakukohde,
      final List<Organisaatio> tarjoajat,
      final List<Valinnantulos> valinnantulos) {
    LOG.info("Muodostetaan Excel valinnantuloksista!");
    Map<String, Valinnantulos> valinnantulokset =
        valinnantulos.stream().collect(Collectors.toMap(Valinnantulos::getHakemusOid, v -> v));
    List<ErillishakuRivi> erillishakuRivit =
        hakemukset.stream()
            .map(
                hakemus -> {
                  Optional<Valinnantulos> tulosOpt =
                      Optional.ofNullable(valinnantulokset.get(hakemus.getOid()));
                  Optional<Maksuntila> maksuntila =
                      lukuvuosimaksus.stream()
                          .filter(l -> hakemus.getApplicationPersonOid().equals(l.getPersonOid()))
                          .map(Lukuvuosimaksu::getMaksuntila)
                          .findAny();
                  return tulosOpt
                      .map(
                          tulos ->
                              createErillishakuRivi(
                                  hakemus.getOid(),
                                  hakemus,
                                  maksuntila,
                                  tulos.getValinnantila().toString(),
                                  new Valintatulos(
                                      hakemus.getOid(),
                                      tulos.getHenkiloOid(),
                                      tulos.getHakukohdeOid(),
                                      haku.oid,
                                      0, // NB: hakutoive aina 0!
                                      BooleanUtils.isTrue(tulos.getHyvaksyttyVarasijalta()),
                                      tulos.getIlmoittautumistila(),
                                      BooleanUtils.isTrue(tulos.getJulkaistavissa()),
                                      tulos.getVastaanottotila(),
                                      BooleanUtils.isTrue(tulos.getEhdollisestiHyvaksyttavissa()),
                                      tulos.getValintatapajonoOid(),
                                      null == tulos.getHyvaksymiskirjeLahetetty()
                                          ? null
                                          : Date.from(
                                              tulos.getHyvaksymiskirjeLahetetty().toInstant()),
                                      tulos.getEhdollisenHyvaksymisenEhtoKoodi(),
                                      tulos.getEhdollisenHyvaksymisenEhtoFI(),
                                      tulos.getEhdollisenHyvaksymisenEhtoSV(),
                                      tulos.getEhdollisenHyvaksymisenEhtoEN()),
                                  tulos.getHakukohdeOid(),
                                  tulos.getValinnantilanKuvauksenTekstiFI(),
                                  tulos.getValinnantilanKuvauksenTekstiSV(),
                                  tulos.getValinnantilanKuvauksenTekstiEN()))
                      .orElse(
                          createErillishakuRivi(
                              hakemus.getOid(),
                              hakemus,
                              maksuntila,
                              "KESKEN",
                              null,
                              tarjontaHakukohde.oid,
                              null,
                              null,
                              null));
                })
            .collect(Collectors.toList());
    return new ErillishakuExcel(
        erillishaku.getHakutyyppi(),
        Teksti.getTeksti(haku.nimi),
        Teksti.getTeksti(tarjontaHakukohde.nimi),
        Teksti.getTeksti(
            tarjoajat.stream().map(Organisaatio::getNimi).collect(Collectors.toList()), " - "),
        erillishakuRivit,
        koodistoCachedAsyncResource);
  }

  private ErillishakuExcel generoiIlmanHakukohdettaJaTuloksia(
      final ErillishakuDTO erillishaku,
      final List<HakemusWrapper> hakemukset,
      final List<Lukuvuosimaksu> lukuvuosimaksus,
      final Haku haku,
      final AbstractHakukohde tarjontaHakukohde,
      final List<Organisaatio> tarjoajat) {
    LOG.info(
        "Hakemuksia ei ole viela tuotu ensimmaistakaan kertaa talle hakukohteelle! Generoidaan hakemuksista excel...");
    Map<String, Maksuntila> personOidToMaksuntila =
        lukuvuosimaksus.stream()
            .collect(Collectors.toMap(l -> l.getPersonOid(), l -> l.getMaksuntila()));
    List<ErillishakuRivi> rivit =
        hakemukset.stream()
            .map(
                hakemus ->
                    createErillishakuRivi(
                        hakemus.getOid(),
                        hakemus,
                        ofNullable(personOidToMaksuntila.get(hakemus.getApplicationPersonOid())),
                        "KESKEN",
                        null,
                        tarjontaHakukohde.oid,
                        null,
                        null,
                        null))
            .collect(Collectors.toList());
    return new ErillishakuExcel(
        erillishaku.getHakutyyppi(),
        Teksti.getTeksti(haku.nimi),
        Teksti.getTeksti(tarjontaHakukohde.nimi),
        Teksti.getTeksti(
            tarjoajat.stream().map(Organisaatio::getNimi).collect(Collectors.toList()), " - "),
        rivit,
        koodistoCachedAsyncResource);
  }

  private ErillishakuRivi createErillishakuRivi(
      String oid,
      HakemusWrapper wrapper,
      Optional<Maksuntila> lukuvuosimaksu,
      String hakemuksenTila,
      Valintatulos valintatulos,
      String hakukohdeOid,
      String valinnantilanKuvauksenTekstiFI,
      String valinnantilanKuvauksenTekstiSV,
      String valinnantilanKuvauksenTekstiEN) {
    ErillishakuRiviBuilder builder =
        new ErillishakuRiviBuilder()
            .hakemusOid(oid)
            .sukunimi(wrapper.getSukunimi())
            .etunimi(wrapper.getEtunimi())
            .henkilotunnus(wrapper.getHenkilotunnus())
            .sahkoposti(wrapper.getSahkopostiOsoite())
            .syntymaAika(wrapper.getSyntymaaikaForErillishaku())
            .sukupuoli(Sukupuoli.fromString(wrapper.getSukupuoliAsIs()))
            .personOid(wrapper.getPersonOid())
            .aidinkieli(wrapper.getAidinkieli())
            .hakemuksenTila(hakemuksenTila)
            .poistetaankoRivi(false)
            .asiointikieli(readAsiointikieli(wrapper))
            .puhelinnumero(wrapper.getPuhelinnumero())
            .osoite(readLahiosoite(wrapper))
            .postinumero(readPostinumero(wrapper))
            .postitoimipaikka(readPostitoimipaikka(wrapper))
            .asuinmaa(wrapper.getAsuinmaa())
            .kansalaisuus(wrapper.getKansalaisuus())
            .kotikunta(readKotikunta(wrapper))
            .toisenAsteenSuoritus(wrapper.getToisenAsteenSuoritus())
            .toisenAsteenSuoritusmaa(wrapper.getToisenAsteenSuoritusmaa())
            .maksuvelvollisuus(wrapper.getMaksuvelvollisuus(hakukohdeOid))
            .maksuntila(
                wrapper.isMaksuvelvollinen(hakukohdeOid)
                    ? lukuvuosimaksu.orElse(Maksuntila.MAKSAMATTA)
                    : null)
            .valinnantilanKuvauksenTekstiFI(valinnantilanKuvauksenTekstiFI)
            .valinnantilanKuvauksenTekstiSV(valinnantilanKuvauksenTekstiSV)
            .valinnantilanKuvauksenTekstiEN(valinnantilanKuvauksenTekstiEN)
            .idTunnus(wrapper.getKansallinenId())
            .syntymapaikka(wrapper.getSyntymapaikka())
            .kaupunkiJaMaa(wrapper.getKaupunkiUlkomaa())
            .passinNumero(wrapper.getPassinnumero());

    if (valintatulos != null) {
      builder
          .ehdollisestiHyvaksyttavissa(valintatulos.getEhdollisestiHyvaksyttavissa())
          .ehdollisenHyvaksymisenEhtoKoodi(valintatulos.getEhdollisenHyvaksymisenEhtoKoodi())
          .ehdollisenHyvaksymisenEhtoFI(valintatulos.getEhdollisenHyvaksymisenEhtoFI())
          .ehdollisenHyvaksymisenEhtoSV(valintatulos.getEhdollisenHyvaksymisenEhtoSV())
          .ehdollisenHyvaksymisenEhtoEN(valintatulos.getEhdollisenHyvaksymisenEhtoEN())
          .hyvaksymiskirjeLahetetty(valintatulos.getHyvaksymiskirjeLahetetty())
          .vastaanottoTila(objectToString(valintatulos.getTila()))
          .ilmoittautumisTila(objectToString(valintatulos.getIlmoittautumisTila()))
          .julkaistaankoTiedot(valintatulos.getJulkaistavissa());
    }

    return builder.build();
  }

  private String readAsiointikieli(HakemusWrapper wrapper) {
    return wrapper.hasAsiointikieli() ? wrapper.getAsiointikieli() : null;
  }

  private String readKotikunta(HakemusWrapper wrapper) {
    String kuntanumero = wrapper.getKotikunta();
    if (isNotBlank(kuntanumero)) {
      Map<String, Koodi> kuntaKoodit =
          koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.KUNTA);
      Koodi postitoimipaikka = kuntaKoodit.get(kuntanumero);
      return KoodistoCachedAsyncResource.haeKoodistaArvo(postitoimipaikka, "FI", null);
    }
    return null;
  }

  private String readPostitoimipaikka(HakemusWrapper wrapper) {
    String suomalainenPostinumero = wrapper.getSuomalainenPostinumero();

    if (isNotBlank(suomalainenPostinumero)) {
      Map<String, Koodi> postiKoodit =
          koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI);
      Koodi postitoimipaikka = postiKoodit.get(suomalainenPostinumero);
      return KoodistoCachedAsyncResource.haeKoodistaArvo(postitoimipaikka, "FI", null);
    }

    return wrapper.getKaupunkiUlkomaa();
  }

  private String readLahiosoite(HakemusWrapper wrapper) {
    return isNotBlank(wrapper.getSuomalainenLahiosoite())
        ? wrapper.getSuomalainenLahiosoite()
        : wrapper.getUlkomainenLahiosoite();
  }

  private String readPostinumero(HakemusWrapper wrapper) {
    return isNotBlank(wrapper.getSuomalainenPostinumero())
        ? wrapper.getSuomalainenPostinumero()
        : wrapper.getUlkomainenPostinumero();
  }
}
