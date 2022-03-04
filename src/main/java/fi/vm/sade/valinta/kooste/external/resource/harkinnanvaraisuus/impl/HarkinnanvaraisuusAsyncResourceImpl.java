package fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.impl;

import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.HarkinnanvaraisuudenSyy;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.HarkinnanvaraisuusAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto.HakemuksenHarkinnanvaraisuus;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto.HakutoiveenHarkinnanvaraisuus;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HarkinnanvaraisuusAsyncResourceImpl implements HarkinnanvaraisuusAsyncResource {
  //    private static final int SUITABLE_ATARU_HAKEMUS_CHUNK_SIZE = 1000;
  //    private final Logger LOG = LoggerFactory.getLogger(getClass());
  //    private final HttpClient client;
  //    private final OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource;
  //    private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;
  //    private final UrlConfiguration urlConfiguration;
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
    //        this.urlConfiguration = UrlConfiguration.getInstance();
    //        this.oppijanumerorekisteriAsyncResource = oppijanumerorekisteriAsyncResource;
    //        this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
  }

  public static final String PK_KOMO = "1.2.246.562.13.62959769647";

  /*public boolean isValmis() {
          return "VALMIS".equals(suoritusJaArvosanat.getSuoritus().getTila());
      }
  */
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
    HakemuksenHarkinnanvaraisuus result = null;
    if (oppija != null
        && !hasValmisPeruskoulu(
            oppija)) { // Todo: lisää leikkuriajanhetki, jota ennen ei vaadita valmista suoritusta /
      // kesken-tilainen riittää tms.
      List<HakutoiveenHarkinnanvaraisuus> hts =
          hakemus.getHakutoiveOids().stream()
              .map(
                  hakukohdeOid ->
                      new HakutoiveenHarkinnanvaraisuus(
                          hakukohdeOid, HarkinnanvaraisuudenSyy.SURE_EI_PAATTOTODISTUSTA))
              .collect(Collectors.toList());
      result = new HakemuksenHarkinnanvaraisuus(hakemus.getOid(), hts);
    } else if (oppija != null && hasYksilollistettyMatAi(oppija)) {
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
          hakemus.ataruHakutoiveet());
      List<HakutoiveenHarkinnanvaraisuus> hts =
          hakemus.ataruHakutoiveet().stream()
              .map(
                  ht ->
                      new HakutoiveenHarkinnanvaraisuus(
                          ht.getHakukohdeOid(), ht.getHarkinnanvaraisuus()))
              .collect(Collectors.toList());
      result = new HakemuksenHarkinnanvaraisuus(hakemus.getOid(), hts);
      LOG.info("Tulos hakemukselle {}: {}", hakemus.getOid(), result);
    }
    return result;
  }

  // Vain atarusta tiedot hakeva toteutus, jota vasten voidaan kehittää valintalaskenta-ui:ta
  public CompletableFuture<List<HakemuksenHarkinnanvaraisuus>>
      getHarkinnanvaraisuudetForHakemuksesOnlyFromAtaru(List<String> hakemusOids) {
    LOG.info("Haetaan harkinnanvaraisuustiedot vain atarusta hakemuksille: {}", hakemusOids);
    CompletableFuture<List<HakemusWrapper>> hakemukset =
        ataruAsyncResource.getApplicationsByOids(hakemusOids);
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

  // WIP
  public CompletableFuture<List<HakemuksenHarkinnanvaraisuus>> getHarkinnanvaraisuudetForHakemukses(
      List<String> hakemusOids) {
    return null;
    /*
    CompletableFuture<List<HakemusWrapper>> hakemukset = ataruAsyncResource.getApplicationsByOids(hakemusOids);
    CompletableFuture<List<HenkiloViiteDto>> viitteet = hakemukset
            .thenComposeAsync(haks -> oppijanumerorekisteriAsyncResource.haeHenkiloOidDuplikaatit(haks.stream().map(HakemusWrapper::getPersonOid).collect(Collectors.toSet())));
    //hakemukset.thenComposeAsync(h -> h.get(0).getHakuoid())
    CompletableFuture<List<Oppija>> suoritukset = suoritusrekisteriAsyncResource.getSuorituksetForOppijasWithoutEnsikertalaisuus(hakemusOids);
    return viitteet.thenComposeAsync(v -> CompletableFuture.completedFuture(null));
    return hakemukset.thenAcceptBoth(suoritukset, (hak, opp) -> {
        CompletableFuture<List<HenkiloViiteDto>> aliakset = oppijanumerorekisteriAsyncResource.haeHenkiloOidDuplikaatit(hak.stream().map(HakemusWrapper::getPersonOid).collect(Collectors.toSet()));
        viitteet.thenComposeAsync(v -> {
           return CompletableFuture.completedFuture(null);
        });
        return viitteet.thenComposeAsync(a -> {
            List<HakemuksenHarkinnanvaraisuus> result = Lists.newArrayList();

            Map<String, List<HakemusWrapper>> hakemuksesByOid = hak.stream().collect(Collectors.groupingBy(HakemusWrapper::getPersonOid, Collectors.mapping(identity(), Collectors.toList())));
            Map<String, List<Oppija>> oppijasByOid = opp.stream().collect(Collectors.groupingBy(Oppija::getOppijanumero, Collectors.mapping(identity(), Collectors.toList())));

            hakemusOids.stream().forEach(hakemusOid -> {
                AtaruHakemusWrapper hakemus = (AtaruHakemusWrapper) hakemuksesByOid.get(hakemusOid).get(0);
                Set<String> henkiloOids = null;
                List<HenkiloViiteDto> matchingAliases = a.stream()
                        .filter(henkiloviite -> henkiloviite.getHenkiloOid().equals(hakemus.getPersonOid()) || henkiloviite.getMasterOid().equals(hakemus.getPersonOid())).collect(Collectors.toList());

                Oppija matchingOppija = opp.stream().filter(o -> matchingAliases.stream()
                        .anyMatch(henkiloViite -> henkiloViite.getMasterOid().equals(o.getOppijanumero())
                                || henkiloViite.getHenkiloOid().equals(o.getOppijanumero()))).findFirst().get();
                if (hakemus != null && matchingOppija != null) {
                    //hakemus.getHakutoivees()
                    HakemuksenHarkinnanvaraisuus hv = syncHarkinnanvaraisuusForHakemus(hakemus, matchingOppija);
                    result.add(hv);
                } else {
                    LOG.warn("Ei hakemusta tai oppijaa!");
                    //throw new RuntimeException("Ei hakemusta tai oppijaa!");
                }

            });
            return CompletableFuture;
            //return CompletableFuture.completedFuture()result;
        });

    });
    //hakemukset.thenComposeAsync(h -> h.forEach())

    return null;*/
  }

  public CompletableFuture<List<HakemuksenHarkinnanvaraisuus>> getSyncedHarkinnanvaraisuudes(
      List<HakemuksenHarkinnanvaraisuus> atarunTiedot) {
    return null;
  }
}
