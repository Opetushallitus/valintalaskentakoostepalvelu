package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.impl;

import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.GenericType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ValintalaskentaValintakoeAsyncResourceImpl extends UrlConfiguredResource
    implements ValintalaskentaValintakoeAsyncResource {
  private final HttpClient httpClient;

  @Autowired
  public ValintalaskentaValintakoeAsyncResourceImpl(
      @Qualifier("ValintalaskentaHttpClient") HttpClient httpClient) {
    super(TimeUnit.HOURS.toMillis(1));
    this.httpClient = httpClient;
  }

  @Override
  public CompletableFuture<List<ValintakoeOsallistuminenDTO>> haeHakutoiveelle(
      String hakukohdeOid) {
    return httpClient.getJson(
        getUrl("valintalaskenta-laskenta-service.valintakoe.hakutoive.hakukohdeoid", hakukohdeOid),
        Duration.ofMinutes(1),
        new GenericType<List<ValintakoeOsallistuminenDTO>>() {}.getType());
  }

  @Override
  public CompletableFuture<List<ValintakoeOsallistuminenDTO>> haeHakutoiveille(
      Collection<String> hakukohdeOids) {
    return httpClient.postJson(
        getUrl("valintalaskenta-laskenta-service.valintakoe.hakutoive"),
        Duration.ofMinutes(5),
        hakukohdeOids,
        new com.google.gson.reflect.TypeToken<List<String>>() {}.getType(),
        new com.google.gson.reflect.TypeToken<List<ValintakoeOsallistuminenDTO>>() {}.getType());
  }

  @Override
  public CompletableFuture<ValintakoeOsallistuminenDTO> haeHakemukselle(String hakemusOid) {
    return httpClient.getJson(
        getUrl("valintalaskenta-laskenta-service.valintakoe.hakemus", hakemusOid),
        Duration.ofMinutes(1),
        new GenericType<ValintakoeOsallistuminenDTO>() {}.getType());
  }

  @Override
  public CompletableFuture<List<HakemusOsallistuminenDTO>> haeValintatiedotHakukohteelle(
      String hakukohdeOid, List<String> valintakoeTunnisteet) {
    return httpClient.postJson(
        getUrl("valintalaskenta-laskenta-service.valintatieto.hakukohde", hakukohdeOid),
        Duration.ofMinutes(5),
        valintakoeTunnisteet,
        new com.google.gson.reflect.TypeToken<List<String>>() {}.getType(),
        new com.google.gson.reflect.TypeToken<List<HakemusOsallistuminenDTO>>() {}.getType());
  }
}
