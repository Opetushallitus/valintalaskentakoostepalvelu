package fi.vm.sade.valinta.kooste.external.resource;

import fi.vm.sade.javautils.cas.ApplicationSession;
import fi.vm.sade.javautils.cas.CasSession;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.impl.TarjontaAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.impl.ValintaTulosServiceAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.sharedutils.http.DateDeserializer;
import java.net.CookieManager;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class HttpClients {
  public static final String CALLER_ID = "1.2.246.562.10.00000000001.valintalaskentakoostepalvelu";

  @Bean
  public CookieManager getCookieManager() {
    return new CookieManager();
  }

  @Bean(name = "CasHttpClient")
  @Autowired
  public java.net.http.HttpClient getCasHttpClient(CookieManager cookieManager) {
    return defaultHttpClientBuilder(cookieManager).build();
  }

  @Bean(name = "AtaruInternalHttpClient")
  @Autowired
  public java.net.http.HttpClient getAtaruInternalHttpClient(CookieManager cookieManager) {
    return defaultHttpClientBuilder(cookieManager).build();
  }

  @Profile("default")
  @Bean(name = "AtaruApplicationSession")
  @Autowired
  public ApplicationSession getAtaruApplicationSession(
      @Qualifier("CasHttpClient") java.net.http.HttpClient casHttpClient,
      @Qualifier("AtaruInternalHttpClient") java.net.http.HttpClient applicationHttpClient,
      CookieManager cookieManager,
      @Value("${cas.service.ataru}") String service,
      @Value("${valintalaskentakoostepalvelu.app.username.to.ataru}") String username,
      @Value("${valintalaskentakoostepalvelu.app.password.to.ataru}") String password) {
    String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets");
    return new ApplicationSession(
        applicationHttpClient,
        cookieManager,
        CALLER_ID,
        Duration.ofSeconds(10),
        new CasSession(
            casHttpClient,
            Duration.ofSeconds(10),
            CALLER_ID,
            URI.create(ticketsUrl),
            username,
            password),
        service,
        "ring-session");
  }

  @Bean(name = "AtaruHttpClient")
  @Autowired
  public HttpClient getAtaruHttpClient(
      @Qualifier("AtaruInternalHttpClient") java.net.http.HttpClient client,
      @Qualifier("AtaruApplicationSession") ApplicationSession applicationSession) {
    return new HttpClient(client, applicationSession, DateDeserializer.gsonBuilder().create());
  }

  @Bean(name = "HakuAppInternalHttpClient")
  @Autowired
  public java.net.http.HttpClient getHakuAppInternalHttpClient(CookieManager cookieManager) {
    return defaultHttpClientBuilder(cookieManager).build();
  }

  @Profile("default")
  @Bean(name = "HakuAppApplicationSession")
  @Autowired
  public ApplicationSession getHakuAppApplicationSession(
      @Qualifier("CasHttpClient") java.net.http.HttpClient casHttpClient,
      @Qualifier("HakuAppInternalHttpClient") java.net.http.HttpClient applicationHttpClient,
      CookieManager cookieManager,
      @Value("${cas.service.haku-service}") String service,
      @Value("${valintalaskentakoostepalvelu.app.username.to.haku}") String username,
      @Value("${valintalaskentakoostepalvelu.app.password.to.haku}") String password) {
    String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets");
    return new ApplicationSession(
        applicationHttpClient,
        cookieManager,
        CALLER_ID,
        Duration.ofSeconds(10),
        new CasSession(
            casHttpClient,
            Duration.ofSeconds(10),
            CALLER_ID,
            URI.create(ticketsUrl),
            username,
            password),
        service,
        "JSESSIONID");
  }

  @Bean(name = "HakuAppHttpClient")
  @Autowired
  public HttpClient getHakuAppHttpClient(
      @Qualifier("HakuAppInternalHttpClient") java.net.http.HttpClient client,
      @Qualifier("HakuAppApplicationSession") ApplicationSession applicationSession) {
    return new HttpClient(client, applicationSession, DateDeserializer.gsonBuilder().create());
  }

  @Bean(name = "TarjontaHttpClient")
  @Autowired
  public HttpClient getTarjontaHttpClient(CookieManager cookieManager) {
    return new HttpClient(
        defaultHttpClientBuilder(cookieManager).build(), null, TarjontaAsyncResourceImpl.getGson());
  }

  @Bean(name = "ValintaTulosServiceHttpClient")
  @Autowired
  public HttpClient getValintaTulosServiceHttpClient(CookieManager cookieManager) {
    return new HttpClient(
        defaultHttpClientBuilder(cookieManager).build(),
        null,
        ValintaTulosServiceAsyncResourceImpl.getGson());
  }

  @Bean(name = "OhjausparametritHttpClient")
  @Autowired
  public HttpClient getOhjausparametritHttpClient(CookieManager cookieManager) {
    return new HttpClient(
        defaultHttpClientBuilder(cookieManager).build(),
        null,
        DateDeserializer.gsonBuilder().create());
  }

  @Bean(name = "OrganisaatioHttpClient")
  @Autowired
  public HttpClient getOrganisaatioHttpClient(CookieManager cookieManager) {
    return new HttpClient(
        defaultHttpClientBuilder(cookieManager).build(),
        null,
        DateDeserializer.gsonBuilder().create());
  }

  @Bean(name = "DokumenttiHttpClient")
  @Autowired
  public HttpClient getDokumenttiHttpClient(CookieManager cookieManager) {
    return new HttpClient(
        defaultHttpClientBuilder(cookieManager).build(),
        null,
        DateDeserializer.gsonBuilder().create());
  }

  @Bean(name = "ViestintapalveluInternalHttpClient")
  @Autowired
  public java.net.http.HttpClient getViestintapalveluInternalHttpClient(
      CookieManager cookieManager) {
    return defaultHttpClientBuilder(cookieManager).build();
  }

  @Profile("default")
  @Bean(name = "ViestintapalveluApplicationSession")
  @Autowired
  public ApplicationSession getViestintapalveluApplicationSession(
      @Qualifier("CasHttpClient") java.net.http.HttpClient casHttpClient,
      @Qualifier("ViestintapalveluInternalHttpClient")
          java.net.http.HttpClient applicationHttpClient,
      CookieManager cookieManager,
      @Value("${cas.service.viestintapalvelu}") String service,
      @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String username,
      @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String password) {
    String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets");
    return new ApplicationSession(
        applicationHttpClient,
        cookieManager,
        CALLER_ID,
        Duration.ofSeconds(10),
        new CasSession(
            casHttpClient,
            Duration.ofSeconds(10),
            CALLER_ID,
            URI.create(ticketsUrl),
            username,
            password),
        service,
        "JSESSIONID");
  }

  @Bean(name = "ViestintapalveluHttpClient")
  @Autowired
  public HttpClient getViestintapalveluHttpClient(
      @Qualifier("ViestintapalveluInternalHttpClient") java.net.http.HttpClient client,
      @Qualifier("ViestintapalveluApplicationSession") ApplicationSession applicationSession) {
    return new HttpClient(client, applicationSession, DateDeserializer.gsonBuilder().create());
  }

  @Bean(name = "KoodistoHttpClient")
  @Autowired
  public HttpClient getKoodistoHttpClient(CookieManager cookieManager) {
    return new HttpClient(
        defaultHttpClientBuilder(cookieManager).build(),
        null,
        DateDeserializer.gsonBuilder().create());
  }

  @Bean(name = "OppijanumerorekisteriInternalHttpClient")
  @Autowired
  public java.net.http.HttpClient getOppijanumerorekisteriInternalHttpClient(
      CookieManager cookieManager) {
    return defaultHttpClientBuilder(cookieManager).build();
  }

  @Profile("default")
  @Bean(name = "OppijanumerorekisteriApplicationSession")
  @Autowired
  public ApplicationSession getOppijanumerorekisteriApplicationSession(
      @Qualifier("CasHttpClient") java.net.http.HttpClient casHttpClient,
      @Qualifier("OppijanumerorekisteriInternalHttpClient")
          java.net.http.HttpClient applicationHttpClient,
      CookieManager cookieManager,
      @Value("${cas.service.oppijanumerorekisteri-service}") String service,
      @Value("${valintalaskentakoostepalvelu.app.username.to.haku}") String username,
      @Value("${valintalaskentakoostepalvelu.app.password.to.haku}") String password) {
    String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets");
    return new ApplicationSession(
        applicationHttpClient,
        cookieManager,
        CALLER_ID,
        Duration.ofSeconds(10),
        new CasSession(
            casHttpClient,
            Duration.ofSeconds(10),
            CALLER_ID,
            URI.create(ticketsUrl),
            username,
            password),
        service,
        "JSESSIONID");
  }

  @Bean(name = "OppijanumerorekisteriHttpClient")
  @Autowired
  public HttpClient getOppijanumerorekisteriHttpClient(
      @Qualifier("OppijanumerorekisteriInternalHttpClient") java.net.http.HttpClient client,
      @Qualifier("OppijanumerorekisteriApplicationSession") ApplicationSession applicationSession) {
    return new HttpClient(client, applicationSession, DateDeserializer.gsonBuilder().create());
  }

  @Bean(name = "ValintapisteServiceHttpClient")
  @Autowired
  public HttpClient getValintapisteServiceHttpClient(CookieManager cookieManager) {
    return new HttpClient(
        defaultHttpClientBuilder(cookieManager).build(),
        null,
        DateDeserializer.gsonBuilder().create());
  }

  @Bean(name = "ValintalaskentaValintakoeHttpClient")
  @Autowired
  public HttpClient getValintalaskentaValintakoeHttpClient(CookieManager cookieManager) {
    return new HttpClient(
        defaultHttpClientBuilder(cookieManager).build(),
        null,
        DateDeserializer.gsonBuilder().create());
  }

  @Bean(name = "SuoritusrekisteriInternalHttpClient")
  @Autowired
  public java.net.http.HttpClient getSuoritusrekisteriInternalHttpClient(
      CookieManager cookieManager) {
    return defaultHttpClientBuilder(cookieManager).build();
  }

  @Profile("default")
  @Bean(name = "SuoritusrekisteriApplicationSession")
  @Autowired
  public ApplicationSession getSuoritusrekisteriApplicationSession(
      @Qualifier("CasHttpClient") java.net.http.HttpClient casHttpClient,
      @Qualifier("SuoritusrekisteriInternalHttpClient")
          java.net.http.HttpClient applicationHttpClient,
      CookieManager cookieManager,
      @Value("${cas.service.suoritusrekisteri}") String service,
      @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String username,
      @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String password) {
    String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets");
    return new ApplicationSession(
        applicationHttpClient,
        cookieManager,
        CALLER_ID,
        Duration.ofSeconds(10),
        new CasSession(
            casHttpClient,
            Duration.ofSeconds(10),
            CALLER_ID,
            URI.create(ticketsUrl),
            username,
            password),
        service,
        "JSESSIONID");
  }

  @Bean(name = "SuoritusrekisteriHttpClient")
  @Autowired
  public HttpClient getSuoritusrekisteriHttpClient(
      @Qualifier("SuoritusrekisteriInternalHttpClient") java.net.http.HttpClient client,
      @Qualifier("SuoritusrekisteriApplicationSession") ApplicationSession applicationSession) {
    return new HttpClient(client, applicationSession, DateDeserializer.gsonBuilder().create());
  }

  @Bean(name = "ValintalaskentaInternalHttpClient")
  @Autowired
  public java.net.http.HttpClient getValintalaskentaInternalHttpClient(
      CookieManager cookieManager) {
    return defaultHttpClientBuilder(cookieManager).build();
  }

  @Profile("default")
  @Bean(name = "ValintalaskentaApplicationSession")
  @Autowired
  public ApplicationSession getValintalaskentaApplicationSession(
      @Qualifier("CasHttpClient") java.net.http.HttpClient casHttpClient,
      @Qualifier("ValintalaskentaInternalHttpClient")
          java.net.http.HttpClient applicationHttpClient,
      CookieManager cookieManager,
      @Value("${cas.service.valintalaskenta-service}") String service,
      @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String username,
      @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String password) {
    String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets");
    return new ApplicationSession(
        applicationHttpClient,
        cookieManager,
        CALLER_ID,
        Duration.ofSeconds(10),
        new CasSession(
            casHttpClient,
            Duration.ofSeconds(10),
            CALLER_ID,
            URI.create(ticketsUrl),
            username,
            password),
        service,
        "JSESSIONID");
  }

  @Bean(name = "ValintalaskentaHttpClient")
  @Autowired
  public HttpClient getValintalaskentaHttpClient(
      @Qualifier("ValintalaskentaInternalHttpClient") java.net.http.HttpClient client,
      @Qualifier("ValintalaskentaApplicationSession") ApplicationSession applicationSession) {
    return new HttpClient(
        client,
        applicationSession,
        DateDeserializer.gsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").create());
  }

  @Bean(name = "ValintaperusteetInternalHttpClient")
  @Autowired
  public java.net.http.HttpClient getValintaperusteetInternalHttpClient(
      CookieManager cookieManager) {
    return defaultHttpClientBuilder(cookieManager).build();
  }

  @Profile("default")
  @Bean(name = "ValintaperusteetApplicationSession")
  @Autowired
  public ApplicationSession getValintaperusteetApplicationSession(
      @Qualifier("CasHttpClient") java.net.http.HttpClient casHttpClient,
      @Qualifier("ValintaperusteetInternalHttpClient")
          java.net.http.HttpClient applicationHttpClient,
      CookieManager cookieManager,
      @Value("${cas.service.valintaperusteet-service}") String service,
      @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String username,
      @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String password) {
    String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets");
    return new ApplicationSession(
        applicationHttpClient,
        cookieManager,
        CALLER_ID,
        Duration.ofSeconds(10),
        new CasSession(
            casHttpClient,
            Duration.ofSeconds(10),
            CALLER_ID,
            URI.create(ticketsUrl),
            username,
            password),
        service,
        "JSESSIONID");
  }

  @Bean(name = "ValintaperusteetHttpClient")
  @Autowired
  public HttpClient getValintaperusteetHttpClient(
      @Qualifier("ValintaperusteetInternalHttpClient") java.net.http.HttpClient client,
      @Qualifier("ValintaperusteetApplicationSession") ApplicationSession applicationSession) {
    return new HttpClient(client, applicationSession, DateDeserializer.gsonBuilder().create());
  }

  public static java.net.http.HttpClient.Builder defaultHttpClientBuilder(
      CookieManager cookieManager) {
    return java.net.http.HttpClient.newBuilder()
        .version(java.net.http.HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(10))
        .cookieHandler(cookieManager);
  }

  @Bean(name = "KoskiHttpClient")
  @Autowired
  public HttpClient getKoskiHttpClient(CookieManager cookieManager) {
    return new HttpClient(
        defaultHttpClientBuilder(cookieManager).build(),
        null,
        DateDeserializer.gsonBuilder().create());
  }
}
