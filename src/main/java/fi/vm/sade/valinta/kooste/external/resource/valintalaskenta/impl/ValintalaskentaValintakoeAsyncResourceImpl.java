package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;
import io.reactivex.Observable;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ValintalaskentaValintakoeAsyncResourceImpl extends UrlConfiguredResource
    implements ValintalaskentaValintakoeAsyncResource {
  private final HttpClient httpClient;

  @Autowired
  public ValintalaskentaValintakoeAsyncResourceImpl(
      @Qualifier("ValintalaskentaValintakoeHttpClient") HttpClient httpClient) {
    super(TimeUnit.HOURS.toMillis(1));
    this.httpClient = httpClient;
  }

  @Override
  public CompletableFuture<List<ValintakoeOsallistuminenDTO>> haeHakutoiveelle(
      String hakukohdeOid) {
    return httpClient.getJson(
        getUrl(
            "valintalaskenta-laskenta-service.valintalaskentakoostepalvelu.valintakoe.hakutoive.hakukohdeoid",
            hakukohdeOid),
        Duration.ofMinutes(1),
        new GenericType<List<ValintakoeOsallistuminenDTO>>() {}.getType());
  }

  @Override
  public Observable<List<ValintakoeOsallistuminenDTO>> haeHakutoiveille(
      Collection<String> hakukohdeOids) {
    return postAsObservableLazily(
        getUrl(
            "valintalaskenta-laskenta-service.valintalaskentakoostepalvelu.valintakoe.hakutoive"),
        new GenericType<List<ValintakoeOsallistuminenDTO>>() {}.getType(),
        Entity.entity(Lists.newArrayList(hakukohdeOids), MediaType.APPLICATION_JSON_TYPE),
        client -> {
          client.accept(MediaType.APPLICATION_JSON_TYPE);
          return client;
        });
  }

  @Override
  public Observable<List<HakemusOsallistuminenDTO>> haeValintatiedotHakukohteelle(
      String hakukohdeOid, List<String> valintakoeTunnisteet) {
    return postAsObservableLazily(
        getUrl(
            "valintalaskenta-laskenta-service.valintalaskentakoostepalvelu.valintatieto.hakukohde",
            hakukohdeOid),
        new TypeToken<List<HakemusOsallistuminenDTO>>() {}.getType(),
        Entity.entity(valintakoeTunnisteet, MediaType.APPLICATION_JSON_TYPE));
  }

  @Override
  public Observable<ValintakoeOsallistuminenDTO> haeHakemukselle(String hakemusOid) {
    return getAsObservableLazily(
        getUrl(
            "valintalaskenta-laskenta-service.valintalaskentakoostepalvelu.valintakoe.hakemus",
            hakemusOid),
        ValintakoeOsallistuminenDTO.class);
  }
}
