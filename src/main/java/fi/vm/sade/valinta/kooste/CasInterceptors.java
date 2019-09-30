package fi.vm.sade.valinta.kooste;

import fi.vm.sade.javautils.cas.ApplicationSession;
import fi.vm.sade.javautils.cas.CasSession;
import fi.vm.sade.valinta.kooste.cas.CasKoosteInterceptor;
import fi.vm.sade.valinta.kooste.external.resource.HttpClients;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

@Profile("default")
@Configuration
public class CasInterceptors {
    private static final String CALLER_ID = "1.2.246.562.10.00000000001.valintalaskentakoostepalvelu";
    private static final String JSESSIONID = "JSESSIONID";

    @Bean(name = "viestintapalveluClientCasInterceptor")
    @Autowired
    public AbstractPhaseInterceptor<Message> getViestintapalveluClientCasInterceptor(
            @Qualifier("CasHttpClient") HttpClient casHttpClient,
            CookieManager cookieManager,
            @Value("${cas.service.viestintapalvelu}") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return getCasInterceptor(casHttpClient, cookieManager, targetService, appClientUsername, appClientPassword);
    }

    @Bean(name = "ryhmasahkopostiClientCasInterceptor")
    @Autowired
    public AbstractPhaseInterceptor<Message> getRyhmasahkopostiClientCasInterceptor(
            @Qualifier("CasHttpClient") HttpClient casHttpClient,
            CookieManager cookieManager,
            @Value("${cas.service.ryhmasahkoposti-service}") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return getCasInterceptor(casHttpClient, cookieManager, targetService, appClientUsername, appClientPassword);
    }

    @Bean(name = "HakemusServiceRestClientAsAdminCasInterceptor")
    @Autowired
    public AbstractPhaseInterceptor<Message> getHakemusServiceRestClientAsAdminCasInterceptor(
            @Qualifier("HakuAppApplicationSession") ApplicationSession applicationSession
    ) {
        return new CasKoosteInterceptor(applicationSession, true);
    }

    @Bean(name = "AuthenticationServiceRestClientCasInterceptor")
    @Autowired
    public AbstractPhaseInterceptor<Message> getAuthenticationServiceRestClientCasInterceptor(
            @Qualifier("CasHttpClient") HttpClient casHttpClient,
            CookieManager cookieManager,
            @Value("${cas.service.authentication-service}") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.haku}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.haku}") String appClientPassword) {
        return getCasInterceptor(casHttpClient, cookieManager, targetService, appClientUsername, appClientPassword);
    }

    @Bean(name = "OppijanumerorekisteriServiceRestClientCasInterceptor")
    @Autowired
    public AbstractPhaseInterceptor<Message> getOppijanumerorekisteriServiceRestClientCasInterceptor(
            @Qualifier("CasHttpClient") HttpClient casHttpClient,
            CookieManager cookieManager,
            @Value("${cas.service.oppijanumerorekisteri-service}") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.haku}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.haku}") String appClientPassword) {
        return getCasInterceptor(casHttpClient, cookieManager, targetService, appClientUsername, appClientPassword);
    }

    @Bean(name = "adminDokumenttipalveluRestClientCasInterceptor")
    @Autowired
    public AbstractPhaseInterceptor<Message> getAdminDokumenttipalveluRestClientCasInterceptor(
            @Qualifier("CasHttpClient") HttpClient casHttpClient,
            CookieManager cookieManager,
            @Value("${cas.service.dokumenttipalvelu}") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintaperusteet}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintaperusteet}") String appClientPassword) {
        return getCasInterceptor(casHttpClient, cookieManager, targetService, appClientUsername, appClientPassword);
    }

    @Bean(name = "ValintalaskentaCasInterceptor")
    @Autowired
    public AbstractPhaseInterceptor<Message> getValintalaskentaCasInterceptor(
            @Qualifier("CasHttpClient") HttpClient casHttpClient,
            CookieManager cookieManager,
            @Value("${cas.service.valintalaskenta-service}") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return getCasInterceptor(casHttpClient, cookieManager, targetService, appClientUsername, appClientPassword);
    }

    @Bean(name = "SeurantaRestClientCasInterceptor")
    @Autowired
    public AbstractPhaseInterceptor<Message> getSeurantaRestClientCasInterceptor(
            @Qualifier("CasHttpClient") HttpClient casHttpClient,
            CookieManager cookieManager,
            @Value("${cas.service.seuranta}") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return getCasInterceptor(casHttpClient, cookieManager, targetService, appClientUsername, appClientPassword);
    }

    @Bean(name = "SuoritusrekisteriRestClientCasInterceptor")
    @Autowired
    public AbstractPhaseInterceptor<Message> getSuoritusrekisteriRestClientCasInterceptor(
            @Qualifier("CasHttpClient") HttpClient casHttpClient,
            CookieManager cookieManager,
            @Value("${cas.service.suoritusrekisteri}") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return getCasInterceptor(casHttpClient, cookieManager, targetService, appClientUsername, appClientPassword);
    }

    @Bean(name = "koodiServiceCasInterceptor")
    @Autowired
    public AbstractPhaseInterceptor<Message> getKoodiServiceCasInterceptor(
            @Qualifier("CasHttpClient") HttpClient casHttpClient,
            CookieManager cookieManager,
            @Value("${cas.service.koodisto-service}") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.koodisto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.koodisto}") String appClientPassword) {
        return getCasInterceptor(casHttpClient, cookieManager, targetService, appClientUsername, appClientPassword);
    }

    @Bean(name = "OrganisaatioResourceClientCasInterceptor")
    @Autowired
    public AbstractPhaseInterceptor<Message> getOrganisaatioResourceClientCasInterceptor(
            @Qualifier("CasHttpClient") HttpClient casHttpClient,
            CookieManager cookieManager,
            @Value("${cas.service.organisaatio-service}") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return getCasInterceptor(casHttpClient, cookieManager, targetService, appClientUsername, appClientPassword);
    }

    private AbstractPhaseInterceptor<Message> getCasInterceptor(
            HttpClient casHttpClient,
            CookieManager cookieManager,
            String service,
            String username,
            String password
    ) {
        String ticketsUrl = UrlConfiguration.getInstance().url("cas.tickets");
        return new CasKoosteInterceptor(
                new ApplicationSession(
                        HttpClients.defaultHttpClientBuilder(cookieManager).build(),
                        cookieManager,
                        CALLER_ID,
                        Duration.ofSeconds(10),
                        new CasSession(casHttpClient, Duration.ofSeconds(10), CALLER_ID, URI.create(ticketsUrl), username, password),
                        service,
                        CasInterceptors.JSESSIONID
                ),
                true
        );
    }
}
