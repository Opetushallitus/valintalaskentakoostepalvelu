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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HarkinnanvaraisuusAsyncResourceImpl implements HarkinnanvaraisuusAsyncResource {

  private static final Logger LOG =
      LoggerFactory.getLogger(HarkinnanvaraisuusAsyncResourceImpl.class);

  private final AtaruAsyncResource ataruAsyncResource;
  private final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;
  private final OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource;

  @Autowired
  public HarkinnanvaraisuusAsyncResourceImpl(
      AtaruAsyncResource ataruAsyncResource,
      SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
      OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource) {
    this.ataruAsyncResource = ataruAsyncResource;
    this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
    this.oppijanumerorekisteriAsyncResource = oppijanumerorekisteriAsyncResource;
  }

  public static final String PK_KOMO = "1.2.246.562.13.62959769647";

  private Boolean hasValmisPeruskoulu(Oppija oppija) {
    return oppija.getSuoritukset().stream()
        .anyMatch(
            sa ->
                PK_KOMO.equals(sa.getSuoritus().getKomo())
                    && "VALMIS".equals(sa.getSuoritus().getTila()));
  }

  private Boolean hasYksilollistettyMatAi(Oppija oppija) {
    return oppija.getSuoritukset().stream()
        .anyMatch(
            sa ->
                PK_KOMO.equals(sa.getSuoritus().getKomo())
                    && sa.getSuoritus().isYksilollistettyMaAi()
                    && "VALMIS".equals(sa.getSuoritus().getTila()));
  }

  private HakemuksenHarkinnanvaraisuus syncHarkinnanvaraisuusForHakemus(
      HakemusWrapper hakemus, Oppija oppija) {
    if (oppija == null) {
      LOG.warn(
          "Hakemuksen {} henkiloOidille {} ei löytynyt suresta Oppijaa!",
          hakemus.getOid(),
          hakemus.getPersonOid());
    }
    HakemuksenHarkinnanvaraisuus result = null;
    if (oppija != null
        && !hasValmisPeruskoulu(
            oppija)) { // Todo: lisää leikkuriajanhetki, jota ennen ei vaadita valmista suoritusta /
      // kesken-tilainen riittää tms.
      LOG.info(
          "Hakemus {} on suren mukaan harkinnanvarainen, koska ei päättötodistusta",
          hakemus.getOid());

      List<HakutoiveenHarkinnanvaraisuus> hts =
          hakemus.getHakutoiveOids().stream()
              .map(
                  hakukohdeOid ->
                      new HakutoiveenHarkinnanvaraisuus(
                          hakukohdeOid, HarkinnanvaraisuudenSyy.SURE_EI_PAATTOTODISTUSTA))
              .collect(Collectors.toList());
      result = new HakemuksenHarkinnanvaraisuus(hakemus.getOid(), hts);
    } else if (oppija != null && hasYksilollistettyMatAi(oppija)) {
      LOG.info("Hakemuksella {} on suressa yksilollistetty MA_AI!", hakemus.getOid());
      List<HakutoiveenHarkinnanvaraisuus> hts =
          hakemus.getHakutoiveOids().stream()
              .map(
                  hakukohdeOid ->
                      new HakutoiveenHarkinnanvaraisuus(
                          hakukohdeOid, HarkinnanvaraisuudenSyy.SURE_YKS_MAT_AI))
              .collect(Collectors.toList());
      result = new HakemuksenHarkinnanvaraisuus(hakemus.getOid(), hts);
    } else {
      LOG.info(
          "Käytetään hakemukselle {} atarun harkinnanvaraisuustietoja: {}",
          hakemus.getOid(),
          hakemus.ataruHakutoiveet().stream()
              .map(ht -> ht.getHakukohdeOid() + " - " + ht.getHarkinnanvaraisuus())
              .collect(Collectors.toList()));
      List<HakutoiveenHarkinnanvaraisuus> hts =
          hakemus.ataruHakutoiveet().stream()
              .map(
                  ht ->
                      new HakutoiveenHarkinnanvaraisuus(
                          ht.getHakukohdeOid(), ht.getHarkinnanvaraisuus()))
              .collect(Collectors.toList());
      result = new HakemuksenHarkinnanvaraisuus(hakemus.getOid(), hts);
    }
    LOG.info(
        "Tulos hakemukselle {}: {}",
        hakemus.getOid(),
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
                  .map(hakemus -> syncHarkinnanvaraisuusForHakemus(hakemus, null))
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
                                              h, findOppijaForHakija(h.getPersonOid(), suor, viit)))
                                  .collect(Collectors.toList());
                          return CompletableFuture.completedFuture(result);
                        })));
  }

  // WIP
  public CompletableFuture<List<HakemuksenHarkinnanvaraisuus>> getSyncedHarkinnanvaraisuudes(
      List<HakemuksenHarkinnanvaraisuus> atarunTiedot) {
    return null;
    /*
        CompletableFuture<List<Oppija>> suoritukset =
                            suoritusrekisteriAsyncResource.getSuorituksetForOppijasWithoutEnsikertalaisuus(
                                    atarunTiedot.stream().map(HakemuksenHarkinnanvaraisuus::getHenkiloOid).collect(Collectors.toList()));

        CompletableFuture<List<HenkiloViiteDto>> viitteet =
                             oppijanumerorekisteriAsyncResource.haeHenkiloOidDuplikaatit(
                                     atarunTiedot.stream().map(HakemuksenHarkinnanvaraisuus::getHenkiloOid).collect(Collectors.toSet()));

        return suoritukset.thenComposeAsync(
                suor -> viitteet.thenComposeAsync(
                                                viit -> {
                                                    LOG.info("synkataan harkinnanvaraisuudet {} hakemukselle ", hak.size());
                                                    List<HakemuksenHarkinnanvaraisuus> result =
                                                            hak.stream()
                                                                    .map(
                                                                            h ->
                                                                                    syncHarkinnanvaraisuusForHakemus(
                                                                                            h, findOppijaForHakija(h.getPersonOid(), suor, viit)))
                                                                    .collect(Collectors.toList());
                                                    return CompletableFuture.completedFuture(result);
                                                })));
    */
  }
}
