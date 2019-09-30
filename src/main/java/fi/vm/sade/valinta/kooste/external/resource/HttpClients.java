package fi.vm.sade.valinta.kooste.external.resource;

import fi.vm.sade.javautils.cas.ApplicationSession;
import fi.vm.sade.javautils.cas.CasSession;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.sharedutils.http.DateDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.CookieManager;
import java.net.URI;
import java.time.Duration;

@Configuration
public class HttpClients {
    private static final String CALLER_ID = "1.2.246.562.10.00000000001.valintalaskentakoostepalvelu";

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
            @Value("${valintalaskentakoostepalvelu.app.password.to.ataru}") String password
    ) {
        String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets");
        return new ApplicationSession(
                applicationHttpClient,
                cookieManager,
                CALLER_ID,
                Duration.ofSeconds(10),
                new CasSession(casHttpClient, Duration.ofSeconds(10), CALLER_ID, URI.create(ticketsUrl), username, password),
                service,
                "ring-session"
        );
    }

    @Bean(name = "AtaruHttpClient")
    @Autowired
    public HttpClient getAtaruHttpClient(
            @Qualifier("AtaruInternalHttpClient") java.net.http.HttpClient client,
            @Qualifier("AtaruApplicationSession") ApplicationSession applicationSession
    ) {
        return new HttpClient(
                client,
                applicationSession,
                DateDeserializer.gsonBuilder().create()
        );
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
            @Value("${valintalaskentakoostepalvelu.app.password.to.haku}") String password
    ) {
        String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets");
        return new ApplicationSession(
                applicationHttpClient,
                cookieManager,
                CALLER_ID,
                Duration.ofSeconds(10),
                new CasSession(casHttpClient, Duration.ofSeconds(10), CALLER_ID, URI.create(ticketsUrl), username, password),
                service,
                "JSESSIONID"
        );
    }

    @Bean(name = "HakuAppHttpClient")
    @Autowired
    public HttpClient getHakuAppHttpClient(
            @Qualifier("HakuAppInternalHttpClient") java.net.http.HttpClient client,
            @Qualifier("HakuAppApplicationSession") ApplicationSession applicationSession
    ) {
        return new HttpClient(
                client,
                applicationSession,
                DateDeserializer.gsonBuilder().create()
        );
    }

    public static java.net.http.HttpClient.Builder defaultHttpClientBuilder(CookieManager cookieManager) {
        return java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .cookieHandler(cookieManager);
    }
}
