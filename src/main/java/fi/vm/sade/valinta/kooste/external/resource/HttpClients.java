package fi.vm.sade.valinta.kooste.external.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.CookieManager;
import java.time.Duration;

@Configuration
public class HttpClients {
    @Bean
    public CookieManager getCookieManager() {
        return new CookieManager();
    }

    @Bean(name = "CasHttpClient")
    @Autowired
    public java.net.http.HttpClient getCasHttpClient(CookieManager cookieManager) {
        return defaultHttpClientBuilder(cookieManager).build();
    }

    public static java.net.http.HttpClient.Builder defaultHttpClientBuilder(CookieManager cookieManager) {
        return java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .cookieHandler(cookieManager);
    }
}
