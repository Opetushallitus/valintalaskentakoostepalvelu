package fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeImportDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RestCasClient;
import io.reactivex.Observable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ValintaperusteetAsyncResourceImpl extends UrlConfiguredResource
    implements ValintaperusteetAsyncResource {
  private static final Logger LOG =
      LoggerFactory.getLogger(ValintaperusteetAsyncResourceImpl.class);
  private final RestCasClient httpClient;

  @Autowired
  public ValintaperusteetAsyncResourceImpl(
      @Qualifier("ValintaperusteetCasInterceptor") AbstractPhaseInterceptor casInterceptor,
      @Qualifier("ValintaperusteetCasClient") RestCasClient httpClient) {
    super(TimeUnit.HOURS.toMillis(1L), casInterceptor);
    this.httpClient = httpClient;
  }

  public Observable<List<ValinnanVaiheJonoillaDTO>> haeIlmanlaskentaa(String hakukohdeOid) {
    LOG.info("Valinnanvaiheiden haku...");
    return getAsObservableLazily(
        getUrl(
            "valintaperusteet-service.valintalaskentakoostepalvelu.hakukohde.ilmanlaskentaa",
            hakukohdeOid),
        new TypeToken<List<ValinnanVaiheJonoillaDTO>>() {}.getType());
  }

  public CompletableFuture<List<ValintaperusteetHakijaryhmaDTO>> haeHakijaryhmat(
      String hakukohdeOid) {
    return httpClient.get(
        getUrl(
            "valintaperusteet-service.valintalaskentakoostepalvelu.valintaperusteet.hakijaryhma",
            hakukohdeOid),
        new com.google.gson.reflect.TypeToken<List<ValintaperusteetHakijaryhmaDTO>>() {},
        Collections.emptyMap(),
        60 * 60 * 1000);
  }

  public CompletableFuture<List<ValintaperusteetDTO>> haeValintaperusteet(
      String hakukohdeOid, Integer valinnanVaiheJarjestysluku) {
    List<Object> parameters = new LinkedList<>();
    parameters.add(hakukohdeOid);
    if (valinnanVaiheJarjestysluku != null) {
      Map<String, String> vaiheParameter = new HashMap<>();
      vaiheParameter.put("vaihe", valinnanVaiheJarjestysluku.toString());
      parameters.add(vaiheParameter);
    }

    String url =
        getUrl(
            "valintaperusteet-service.valintalaskentakoostepalvelu.valintaperusteet",
            parameters.toArray());

    return httpClient.get(
        url,
        new com.google.gson.reflect.TypeToken<List<ValintaperusteetDTO>>() {},
        Collections.emptyMap(),
        60 * 60 * 1000);
  }

  public Observable<List<HakukohdeViiteDTO>> haunHakukohteet(String hakuOid) {
    return getAsObservableLazily(
        getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.hakukohde.haku", hakuOid),
        new TypeToken<List<HakukohdeViiteDTO>>() {}.getType(),
        ACCEPT_JSON);
  }

  @Override
  public Observable<Response> tuoHakukohde(HakukohdeImportDTO hakukohde) {
    return postAsObservableLazily(
        getUrl(
            "valintaperusteet-service.valintalaskentakoostepalvelu.valintaperusteet.tuohakukohde"),
        Entity.entity(hakukohde, MediaType.APPLICATION_JSON_TYPE),
        ACCEPT_JSON);
  }

  @Override
  public CompletableFuture<List<ValintaperusteDTO>> findAvaimet(String hakukohdeOid) {
    return httpClient.get(
        getUrl(
            "valintaperusteet-service.valintalaskentakoostepalvelu.hakukohde.avaimet.oid",
            hakukohdeOid),
        new com.google.gson.reflect.TypeToken<List<ValintaperusteDTO>>() {},
        Collections.emptyMap(),
        60 * 60 * 1000);
  }

  @Override
  public Observable<List<HakukohdeJaValintaperusteDTO>> findAvaimet(
      Collection<String> hakukohdeOids) {
    return postAsObservableLazily(
        getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.hakukohde.avaimet"),
        new TypeToken<List<HakukohdeJaValintaperusteDTO>>() {}.getType(),
        Entity.entity(Lists.newArrayList(hakukohdeOids), MediaType.APPLICATION_JSON_TYPE),
        client -> {
          client.accept(MediaType.APPLICATION_JSON_TYPE);
          return client;
        });
  }

  @Override
  public Observable<List<ValintaperusteetDTO>> valintaperusteet(String valinnanvaiheOid) {
    return getAsObservableLazily(
        getUrl(
            "valintaperusteet-service.valintalaskentakoostepalvelu.valinnanvaihe.valintaperusteet",
            valinnanvaiheOid),
        new TypeToken<List<ValintaperusteetDTO>>() {}.getType());
  }

  @Override
  public Observable<List<HakukohdeJaValintakoeDTO>> haeValintakokeetHakukohteille(
      Collection<String> hakukohdeOids) {
    return postAsObservableLazily(
        getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.hakukohde.valintakoe"),
        new GenericType<List<HakukohdeJaValintakoeDTO>>() {}.getType(),
        Entity.entity(hakukohdeOids, MediaType.APPLICATION_JSON_TYPE),
        ACCEPT_JSON);
  }

  @Override
  public Observable<List<HakukohdeJaValintakoeDTO>> haeValintakokeetHakutoiveille(
      Collection<String> hakukohdeOids) {
    return postAsObservableLazily(
        getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.hakukohde.valintakoe"),
        new TypeToken<List<HakukohdeJaValintakoeDTO>>() {}.getType(),
        Entity.entity(Lists.newArrayList(hakukohdeOids), MediaType.APPLICATION_JSON_TYPE),
        client -> {
          client.accept(MediaType.APPLICATION_JSON_TYPE);
          return client;
        });
  }

  @Override
  public Observable<Map<String, List<ValintatapajonoDTO>>> haeValintatapajonotSijoittelulle(
      Collection<String> hakukohdeOids) {
    return postAsObservableLazily(
        getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.valintatapajono"),
        new TypeToken<Map<String, List<ValintatapajonoDTO>>>() {}.getType(),
        Entity.entity(hakukohdeOids, MediaType.APPLICATION_JSON_TYPE));
  }

  @Override
  public Observable<List<ValintakoeDTO>> haeValintakokeetHakukohteelle(String hakukohdeOid) {
    return getAsObservableLazily(
        getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.valintakoe", hakukohdeOid),
        new GenericType<List<ValintakoeDTO>>() {}.getType(),
        ACCEPT_JSON);
  }

  @Override
  public Observable<Set<String>> haeHakukohteetValinnanvaiheelle(String oid) {
    String url =
        getUrl(
            "valintaperusteet-service.valintalaskentakoostepalvelu.valinnanvaihe.hakukohteet", oid);
    LOG.info("Calling url {}", url);
    return getAsObservableLazily(url, new TypeToken<Set<String>>() {}.getType());
  }

  @Override
  public Observable<String> haeValintaryhmaVastuuorganisaatio(String valintaryhmaOid) {
    String url =
        getUrl(
            "valintaperusteet-service.valintalaskentakoostepalvelu.valintaryhma.vastuuorganisaatio",
            valintaryhmaOid);
    LOG.info("Calling url {}", url);
    return getAsObservableLazily(
        url,
        String.class,
        client -> {
          client.accept(MediaType.TEXT_PLAIN_TYPE);
          return client;
        });
  }
}
