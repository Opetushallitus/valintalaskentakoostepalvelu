package fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.impl;

import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.HarkinnanvaraisuudenSyy;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.HarkinnanvaraisuusAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto.HakemuksenHarkinnanvaraisuus;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto.HakutoiveenHarkinnanvaraisuus;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloViiteDto;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HarkinnanvaraisuusAsyncResourceImpl implements HarkinnanvaraisuusAsyncResource {

  private static final Logger LOG =
      LoggerFactory.getLogger(HarkinnanvaraisuusAsyncResourceImpl.class);

  private final AtaruAsyncResource ataruAsyncResource;
  private final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;
  private final OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource;
  private final LocalDateTime suoritusValmisDeadline;

  @Autowired
  public HarkinnanvaraisuusAsyncResourceImpl(
      @Value("${valintalaskentakoostepalvelu.harkinnanvaraisuus.paattely.leikkuripvm:2022-06-06}")
          String suoritusValmisDeadlinePvm,
      AtaruAsyncResource ataruAsyncResource,
      SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
      OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource) {
    this.ataruAsyncResource = ataruAsyncResource;
    this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
    this.oppijanumerorekisteriAsyncResource = oppijanumerorekisteriAsyncResource;
    this.suoritusValmisDeadline = LocalDate.parse(suoritusValmisDeadlinePvm).atStartOfDay();
  }

  public static final String PK_KOMO = "1.2.246.562.13.62959769647";

  // todo ota huomioon myös muita pohjakoulutuskomoja
  private Boolean hasValmisPeruskoulu(Oppija oppija) {
    if (oppija != null) {
      return oppija.getSuoritukset().stream()
          .anyMatch(
              sa ->
                  PK_KOMO.equals(sa.getSuoritus().getKomo())
                      && "VALMIS".equals(sa.getSuoritus().getTila())
                      && sa.getSuoritus().isVahvistettu());
    } else {
      return false;
    }
  }

  private Boolean hasYksilollistettyMatAi(Oppija oppija) {
    if (oppija != null) {
      return oppija.getSuoritukset().stream()
          .anyMatch(
              sa ->
                  PK_KOMO.equals(sa.getSuoritus().getKomo())
                      && sa.getSuoritus().isYksilollistettyMaAi()
                      && sa.getSuoritus().isVahvistettu());

    } else {
      return false;
    }
  }

  private HakemuksenHarkinnanvaraisuus syncHarkinnanvaraisuusForHakemus(
      String hakemusOid,
      String henkiloOidHakemukselta,
      List<HakutoiveenHarkinnanvaraisuus> tiedotAtarusta,
      Oppija oppija) {
    if (oppija == null) {
      LOG.warn(
          "Hakemuksen {} henkiloOidille {} ei löytynyt suresta Oppijaa!",
          hakemusOid,
          henkiloOidHakemukselta);
    }
    HakemuksenHarkinnanvaraisuus result = null;
    if (LocalDateTime.now().isAfter(suoritusValmisDeadline) && !hasValmisPeruskoulu(oppija)) {
      LOG.info(
          "Hakemus {} on suren mukaan harkinnanvarainen, koska ei päättötodistusta", hakemusOid);
      tiedotAtarusta.forEach(
          ht -> ht.setHarkinnanvaraisuudenSyy(HarkinnanvaraisuudenSyy.SURE_EI_PAATTOTODISTUSTA));
      result = new HakemuksenHarkinnanvaraisuus(hakemusOid, tiedotAtarusta);
    } else if (hasYksilollistettyMatAi(oppija)) {
      LOG.info("Hakemuksella {} on suressa yksilollistetty MA_AI!", hakemusOid);
      tiedotAtarusta.forEach(
          ht -> ht.setHarkinnanvaraisuudenSyy(HarkinnanvaraisuudenSyy.SURE_YKS_MAT_AI));
      result = new HakemuksenHarkinnanvaraisuus(hakemusOid, tiedotAtarusta);
    } else {
      LOG.info(
          "Käytetään hakemukselle {} atarun harkinnanvaraisuustietoja: {}",
          hakemusOid,
          tiedotAtarusta.stream()
              .map(ht -> ht.getHakukohdeOid() + " - " + ht.getHarkinnanvaraisuudenSyy())
              .collect(Collectors.toList()));
      tiedotAtarusta.forEach(
          hh -> {
            if (hh.getHarkinnanvaraisuudenSyy()
                    .equals(HarkinnanvaraisuudenSyy.ATARU_EI_PAATTOTODISTUSTA)
                && hasValmisPeruskoulu(oppija)) {
              LOG.info(
                  "Hakemuksella {} harkinnanvaraiseksi merkitty hakutoive {} ei ole harkinnanvarainen, koska suresta löytyy valmis pohjakoulutus!",
                  hakemusOid,
                  hh.getHakukohdeOid());
              hh.setHarkinnanvaraisuudenSyy(HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN);
            }
          });
      result = new HakemuksenHarkinnanvaraisuus(hakemusOid, tiedotAtarusta);
    }
    LOG.info(
        "Harkinnanvaraisuuden tulos hakemukselle {}: {}",
        hakemusOid,
        result.getHakutoiveet().stream()
            .map(hh -> hh.getHakukohdeOid() + " - " + hh.getHarkinnanvaraisuudenSyy())
            .collect(Collectors.toList()));
    return result;
  }

  // Vain atarusta tiedot hakeva toteutus, jota vasten voidaan kehittää valintalaskenta-ui:ta
  public CompletableFuture<List<HakemuksenHarkinnanvaraisuus>>
      getHarkinnanvaraisuudetForHakemuksesOnlyFromAtaru(List<String> hakemusOids) {
    LOG.info("Haetaan harkinnanvaraisuustiedot vain atarusta hakemuksille: {}", hakemusOids);
    CompletableFuture<List<HakemusWrapper>> hakemukset =
        ataruAsyncResource.getApplicationsByOidsWithHarkinnanvaraisuustieto(hakemusOids);
    try {
      return hakemukset.thenApply(
          h ->
              h.stream()
                  .map(
                      hakemus ->
                          syncHarkinnanvaraisuusForHakemus(
                              hakemus.getOid(),
                              hakemus.getPersonOid(),
                              hakemus.ataruHakutoiveet().stream()
                                  .map(
                                      ht ->
                                          new HakutoiveenHarkinnanvaraisuus(
                                              ht.getHakukohdeOid(), ht.getHarkinnanvaraisuus()))
                                  .collect(Collectors.toList()),
                              null))
                  .collect(Collectors.toList()));
    } catch (Exception e) {
      LOG.error("Virhe haettaessa harkinnanvaraisuustietoja:", e);
      return CompletableFuture.failedFuture(e);
    }
  }

  private Oppija findOppijaForHakija(
      String oidFromHakemus, List<Oppija> oppijas, List<HenkiloViiteDto> henkiloviittees) {
    List<String> aliakset = new ArrayList<>();
    aliakset.add(oidFromHakemus);
    List<HenkiloViiteDto> hakijanViitteet =
        henkiloviittees.stream()
            .filter(hv -> List.of(hv.getHenkiloOid(), hv.getMasterOid()).contains(oidFromHakemus))
            .collect(Collectors.toList());
    hakijanViitteet.forEach(
        viite -> aliakset.addAll(List.of(viite.getHenkiloOid(), viite.getMasterOid())));
    Optional<Oppija> o =
        oppijas.stream().filter(oppija -> aliakset.contains(oppija.getOppijanumero())).findFirst();
    LOG.info("Hakemukselle {} aliaksia yhteensä {}", oidFromHakemus, aliakset.size());
    return o.orElse(null);
  }

  public CompletableFuture<List<HakemuksenHarkinnanvaraisuus>> getHarkinnanvaraisuudetForHakemukses(
      List<String> hakemusOids) {
    LOG.info("Haetaan harkinnanvaraisuudet hakemuksille: {}", hakemusOids);
    CompletableFuture<List<HakemusWrapper>> hakemukset =
        ataruAsyncResource.getApplicationsByOidsWithHarkinnanvaraisuustieto(hakemusOids);

    CompletableFuture<List<Oppija>> suoritukset =
        hakemukset.thenComposeAsync(
            h -> {
              LOG.info("Saatiin Atarusta {} hakemusta, haetaan suoritukset hakijoille", h.size());
              return suoritusrekisteriAsyncResource.getSuorituksetForOppijasWithoutEnsikertalaisuus(
                  h.stream().map(HakemusWrapper::getPersonOid).collect(Collectors.toList()));
            });

    CompletableFuture<List<HenkiloViiteDto>> viitteet =
        hakemukset.thenComposeAsync(
            h -> {
              LOG.info("Saatiin Atarusta {} hakemusta, haetaan henkilöviitteet", h.size());
              return oppijanumerorekisteriAsyncResource.haeHenkiloOidDuplikaatit(
                  h.stream().map(HakemusWrapper::getPersonOid).collect(Collectors.toSet()));
            });

    return hakemukset.thenComposeAsync(
        hak ->
            suoritukset.thenComposeAsync(
                suor ->
                    viitteet.thenComposeAsync(
                        viit -> {
                          LOG.info(
                              "synkataan harkinnanvaraisuudet {} hakemukselle, oppijoita {}, henkiloViitteita {}",
                              hak.size(),
                              suor.size(),
                              viit.size());
                          List<HakemuksenHarkinnanvaraisuus> result =
                              hak.stream()
                                  .map(
                                      h ->
                                          syncHarkinnanvaraisuusForHakemus(
                                              h.getOid(),
                                              h.getPersonOid(),
                                              h.ataruHakutoiveet().stream()
                                                  .map(
                                                      ht ->
                                                          new HakutoiveenHarkinnanvaraisuus(
                                                              ht.getHakukohdeOid(),
                                                              ht.getHarkinnanvaraisuus()))
                                                  .collect(Collectors.toList()),
                                              findOppijaForHakija(h.getPersonOid(), suor, viit)))
                                  .collect(Collectors.toList());
                          return CompletableFuture.completedFuture(result);
                        })));
  }

  public CompletableFuture<List<HakemuksenHarkinnanvaraisuus>> getSyncedHarkinnanvaraisuudes(
      List<HakemuksenHarkinnanvaraisuus> atarunTiedot) {
    CompletableFuture<List<Oppija>> suoritukset =
        suoritusrekisteriAsyncResource.getSuorituksetForOppijasWithoutEnsikertalaisuus(
            atarunTiedot.stream()
                .map(HakemuksenHarkinnanvaraisuus::getHenkiloOid)
                .collect(Collectors.toList()));

    CompletableFuture<List<HenkiloViiteDto>> viitteet =
        oppijanumerorekisteriAsyncResource.haeHenkiloOidDuplikaatit(
            atarunTiedot.stream()
                .map(HakemuksenHarkinnanvaraisuus::getHenkiloOid)
                .collect(Collectors.toSet()));

    return suoritukset.thenComposeAsync(
        suor ->
            viitteet.thenComposeAsync(
                viit -> {
                  LOG.info("synkataan harkinnanvaraisuudet {} hakemukselle ", atarunTiedot.size());
                  List<HakemuksenHarkinnanvaraisuus> result =
                      atarunTiedot.stream()
                          .map(
                              hh ->
                                  syncHarkinnanvaraisuusForHakemus(
                                      hh.getHakemusOid(),
                                      hh.getHenkiloOid(),
                                      hh.getHakutoiveet(),
                                      findOppijaForHakija(hh.getHenkiloOid(), suor, viit)))
                          .collect(Collectors.toList());
                  return CompletableFuture.completedFuture(result);
                }));
  }
}