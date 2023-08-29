package fi.vm.sade.valinta.kooste.external.resource;

import static fi.vm.sade.valinta.sharedutils.http.HttpResource.CSRF_VALUE;

import fi.vm.sade.javautils.nio.cas.CasConfig;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.impl.TarjontaAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.impl.ValintaTulosServiceAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RestCasClient;
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
  @Bean(name = "AtaruCasClient")
  @Autowired
  public RestCasClient getAtaruCasClient(
      @Value("${cas.service.ataru}") String service,
      @Value("${valintalaskentakoostepalvelu.app.username.to.ataru}") String username,
      @Value("${valintalaskentakoostepalvelu.app.password.to.ataru}") String password) {
    String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets.new");
    return new RestCasClient(
        CasConfig.CasConfig(
            username, password, ticketsUrl, service, CSRF_VALUE, CALLER_ID, "ring-session", ""));
  }

  @Bean(name = "HakuAppInternalHttpClient")
  @Autowired
  public java.net.http.HttpClient getHakuAppInternalHttpClient(CookieManager cookieManager) {
    return defaultHttpClientBuilder(cookieManager).build();
  }

  @Bean(name = "SijoitteluServiceInternalHttpClient")
  @Autowired
  public java.net.http.HttpClient getSijoitteluServiceInternalHttpClient(
      CookieManager cookieManager) {
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

  @Profile("default")
  @Bean(name = "HakuAppCasClient")
  @Autowired
  public RestCasClient getHakuAppCasClient(
      @Value("${cas.service.haku-service}") String service,
      @Value("${valintalaskentakoostepalvelu.app.username.to.haku}") String username,
      @Value("${valintalaskentakoostepalvelu.app.password.to.haku}") String password) {
    String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets.new");
    return new RestCasClient(
        CasConfig.CasConfig(
            username, password, ticketsUrl, service, CSRF_VALUE, CALLER_ID, "JSESSIONID", ""));
  }

  @Bean(name = "SijoitteluServiceApplicationSession")
  @Autowired
  public ApplicationSession getSijoitteluServiceApplicationSession(
      @Qualifier("CasHttpClient") java.net.http.HttpClient casHttpClient,
      @Qualifier("SijoitteluServiceInternalHttpClient")
          java.net.http.HttpClient applicationHttpClient,
      CookieManager cookieManager,
      @Value("${cas.service.sijoittelu-service}") String service,
      @Value("${valintalaskentakoostepalvelu.app.username.to.sijoittelu}") String username,
      @Value("${valintalaskentakoostepalvelu.app.password.to.sijoittelu}") String password) {
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

  @Profile("default")
  @Bean(name = "SijoitteluServiceCasClient")
  @Autowired
  public RestCasClient getSijoitteluServiceCasClient(
      @Value("${cas.service.sijoittelu-service}") String service,
      @Value("${valintalaskentakoostepalvelu.app.username.to.sijoittelu}") String username,
      @Value("${valintalaskentakoostepalvelu.app.password.to.sijoittelu}") String password) {
    String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets.new");
    return new RestCasClient(
        CasConfig.CasConfig(
            username, password, ticketsUrl, service, CSRF_VALUE, CALLER_ID, "JSESSIONID", ""));
  }

  @Bean(name = "TarjontaHttpClient")
  @Autowired
  public HttpClient getTarjontaHttpClient(CookieManager cookieManager) {
    return new HttpClient(
        defaultHttpClientBuilder(cookieManager).build(), null, TarjontaAsyncResourceImpl.getGson());
  }

  @Bean(name = "HakukohderyhmapalveluInternalHttpClient")
  @Autowired
  public java.net.http.HttpClient getHakukohderyhmapalveluInternalHttpClient(
      CookieManager cookieManager) {
    return defaultHttpClientBuilder(cookieManager).build();
  }

  @Profile("default")
  @Bean(name = "HakukohderyhmapalveluCasClient")
  @Autowired
  public RestCasClient getHakukohderyhmapalveluCasClient(
      @Value("${valintalaskentakoostepalvelu.app.username.to.hakukohderyhmapalvelu}")
          String username,
      @Value("${valintalaskentakoostepalvelu.app.password.to.hakukohderyhmapalvelu}")
          String password) {
    String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets.new");
    String service = UrlConfiguration.getInstance().url("hakukohderyhmapalvelu.auth.login");
    return new RestCasClient(
        CasConfig.CasConfig(
            username, password, ticketsUrl, service, CSRF_VALUE, CALLER_ID, "ring-session", ""));
  }

  @Bean(name = "KoutaInternalHttpClient")
  @Autowired
  public java.net.http.HttpClient getKoutaInternalHttpClient(CookieManager cookieManager) {
    return defaultHttpClientBuilder(cookieManager).build();
  }

  @Profile("default")
  @Bean(name = "KoutaCasClient")
  @Autowired
  public RestCasClient getKoutaCasClient(
      @Value("${valintalaskentakoostepalvelu.app.username.to.kouta-internal}") String username,
      @Value("${valintalaskentakoostepalvelu.app.password.to.kouta-internal}") String password) {
    String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets.new");
    String service = UrlConfiguration.getInstance().url("kouta-internal.auth.login");
    return new RestCasClient(
        CasConfig.CasConfig(
            username, password, ticketsUrl, service, CSRF_VALUE, CALLER_ID, "session", ""));
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

  @Profile("default")
  @Bean(name = "ViestintapalveluCasClient")
  @Autowired
  public RestCasClient getViestintapalveluCasClient(
      @Value("${cas.service.viestintapalvelu}") String service,
      @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String username,
      @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String password) {
    String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets.new");
    return new RestCasClient(
        CasConfig.CasConfig(
            username, password, ticketsUrl, service, CSRF_VALUE, CALLER_ID, "JSESSIONID", ""));
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

  @Profile("default")
  @Bean(name = "OppijanumerorekisteriCasClient")
  @Autowired
  public RestCasClient getOppijanumerorekisteriCasClient(
      @Value("${cas.service.oppijanumerorekisteri-service}") String service,
      @Value("${valintalaskentakoostepalvelu.app.username.to.haku}") String username,
      @Value("${valintalaskentakoostepalvelu.app.password.to.haku}") String password) {
    String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets.new");
    return new RestCasClient(
        CasConfig.CasConfig(
            username, password, ticketsUrl, service, CSRF_VALUE, CALLER_ID, "JSESSIONID", ""));
  }

  @Bean(name = "ValintapisteServiceInternalHttpClient")
  @Autowired
  public java.net.http.HttpClient getValintapisteServiceInternalHttpClient(
      CookieManager cookieManager) {
    return defaultHttpClientBuilder(cookieManager).build();
  }

  @Profile("default")
  @Bean(name = "ValintapisteServiceCasClient")
  @Autowired
  public RestCasClient getValintapisteServiceCasClient(
      @Value("${cas.service.valintapiste-service}") String service,
      @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String username,
      @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String password) {
    String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets.new");
    return new RestCasClient(
        CasConfig.CasConfig(
            username, password, ticketsUrl, service, CSRF_VALUE, CALLER_ID, "ring-session", ""));
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
  @Bean(name = "SuoritusrekisteriCasClient")
  @Autowired
  public RestCasClient getSuoritusrekisteriCasClient(
      @Value("${cas.service.suoritusrekisteri}") String service,
      @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String username,
      @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String password) {
    String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets.new");
    return new RestCasClient(
        CasConfig.CasConfig(
            username, password, ticketsUrl, service, CSRF_VALUE, CALLER_ID, "JSESSIONID", ""));
  }

  @Bean(name = "ValintalaskentaInternalHttpClient")
  @Autowired
  public java.net.http.HttpClient getValintalaskentaInternalHttpClient(
      CookieManager cookieManager) {
    return defaultHttpClientBuilder(cookieManager).build();
  }

  @Profile("default")
  @Bean(name = "ValintalaskentaCasClient")
  @Autowired
  public RestCasClient getValintalaskentaCasClient(
      @Value("${cas.service.valintalaskenta-service}") String service,
      @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String username,
      @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String password) {
    String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets.new");
    return new RestCasClient(
        CasConfig.CasConfig(
            username, password, ticketsUrl, service, CSRF_VALUE, CALLER_ID, "JSESSIONID", ""),
        DateDeserializer.gsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").create());
  }

  @Bean(name = "ValintaperusteetInternalHttpClient")
  @Autowired
  public java.net.http.HttpClient getValintaperusteetInternalHttpClient(
      CookieManager cookieManager) {
    return defaultHttpClientBuilder(cookieManager).build();
  }

  @Profile("default")
  @Bean(name = "ValintaperusteetCasClient")
  @Autowired
  public RestCasClient getValintaperusteetCasClient(
      @Value("${cas.service.valintaperusteet-service}") String service,
      @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String username,
      @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String password) {
    String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets.new");
    return new RestCasClient(
        CasConfig.CasConfig(
            username, password, ticketsUrl, service, CSRF_VALUE, CALLER_ID, "JSESSIONID", ""));
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
