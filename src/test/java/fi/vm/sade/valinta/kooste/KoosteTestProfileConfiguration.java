package fi.vm.sade.valinta.kooste;

import fi.vm.sade.javautils.cas.ApplicationSession;
import fi.vm.sade.javautils.cas.ServiceTicket;
import fi.vm.sade.javautils.cas.SessionToken;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.net.HttpCookie;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;


@Profile("test")
@Configuration
public class KoosteTestProfileConfiguration {

    public static AtomicReference<String> PROXY_SERVER = new AtomicReference<>();

    @Bean(name = "testProps")
    public static org.springframework.context.support.PropertySourcesPlaceholderConfigurer getPropertyPlaceholderConfigurer() {
        final String proxyServer = PROXY_SERVER.get();
        Properties p0 = new Properties();
        p0.setProperty("valintalaskentakoostepalvelu.jatkuvasijoittelu.timer", "time=2018-12-12 10:12:12&delay=10000000");
        p0.setProperty("valintalaskentakoostepalvelu.valintalaskenta.rest.url", "http://" + proxyServer + "/valintalaskenta-laskenta-service/resources");
        p0.setProperty("valintalaskentakoostepalvelu.ryhmasahkoposti.url", "http://" + proxyServer + "/ryhmasahkoposti-service");
        p0.setProperty("valintalaskentakoostepalvelu.viestintapalvelu.url", "http://" + proxyServer + "/viestintapalvelu");
        p0.setProperty("valintalaskentakoostepalvelu.hakemus.rest.url", "http://" + proxyServer + "/haku-app");
        p0.setProperty("valintalaskentakoostepalvelu.koodiService.url", "http://localhost");
        p0.setProperty("cas.callback.valintalaskentakoostepalvelu", "http://localhost");
        p0.setProperty("valintalaskentakoostepalvelu.dokumenttipalvelu.rest.url", "http://localhost");
        p0.setProperty("valintalaskentakoostepalvelu.valintatulosservice.rest.url",Optional.ofNullable(System.getProperty("vts_server")).orElse("http://" + proxyServer) + "/valinta-tulos-service");
        p0.setProperty("valintalaskentakoostepalvelu.sijoittelu.rest.url",Optional.ofNullable(System.getProperty("sijoittelu_server")).orElse("http://" + proxyServer) + "/sijoittelu-service/resources");

        p0.setProperty("valintalaskentakoostepalvelu.seuranta.rest.url", "http://localhost");
        p0.setProperty("valintalaskentakoostepalvelu.organisaatioService.rest.url", "http://" + proxyServer + "/organisaatio-service/rest");
        p0.setProperty("valintalaskentakoostelvelu.organisaatio-service-url", "http://" + proxyServer + "/organisaatio-service");
        p0.setProperty("valintalaskentakoostepalvelu.tarjonta.rest.url", "http://" + proxyServer + "/tarjonta-service/rest");
        p0.setProperty("valintalaskentakoostepalvelu.koodisto.url", "https://itest-virkailija.oph.ware.fi/");
        p0.setProperty("valintalaskentakoostepalvelu.tarjontaService.url", "http://localhost");
        p0.setProperty("valintalaskentakoostepalvelu.valintaperusteet.rest.url", "http://localhost");
        p0.setProperty("valintalaskentakoostepalvelu.oppijantunnistus.rest.url", "http://" + proxyServer + "/oppijan-tunnistus");
        p0.setProperty("valintalaskentakoostepalvelu.kirjeet.polling.interval.millis", "50");
        p0.setProperty("root.organisaatio.oid", "");
        p0.setProperty("kela.ftp.protocol", "ftp");
        p0.setProperty("kela.ftp.username", "username");
        p0.setProperty("kela.ftp.password", "password");
        p0.setProperty("kela.ftp.parameters", "");
        p0.setProperty("kela.ftp.host", "host");
        p0.setProperty("kela.ftp.port", "22");
        p0.setProperty("kela.ftp.path", "/");

        p0.setProperty("host.ilb", "http://" + proxyServer);

        p0.setProperty("web.url.cas", "http://localhost");
        p0.setProperty("cas.service.viestintapalvelu", "");
        p0.setProperty("cas.service.sijoittelu-service", "");
        p0.setProperty("cas.service.organisaatio-service", "");
        p0.setProperty("cas.service.valintalaskenta-service", "");
        p0.setProperty("cas.service.dokumenttipalvelu", "");
        p0.setProperty("valintalaskentakoostepalvelu.swagger.basepath", "/valintalaskentakoostepalvelu/resources");
        p0.setProperty("host.scheme", "http");
        p0.setProperty("host.virkailija", "" + proxyServer);
        p0.setProperty("cas.service.valintalaskentakoostepalvelu", "");
        p0.setProperty("cas.service.haku-service", "");
        p0.setProperty("cas.service.authentication-service", "");
        p0.setProperty("cas.service.oppijanumerorekisteri-service", "");
        p0.setProperty("valintalaskentakoostepalvelu.authentication.rest.url", "");
        p0.setProperty("valintalaskentakoostepalvelu.app.username.to.sijoittelu", "");
        p0.setProperty("valintalaskentakoostepalvelu.app.password.to.sijoittelu", "");
        p0.setProperty("valintalaskentakoostepalvelu.app.username.to.valintatieto", "");
        p0.setProperty("valintalaskentakoostepalvelu.app.password.to.valintatieto", "");
        p0.setProperty("valintalaskentakoostepalvelu.app.username.to.haku", "");
        p0.setProperty("valintalaskentakoostepalvelu.app.password.to.haku", "");
        p0.setProperty("valintalaskentakoostepalvelu.app.username.to.valintaperusteet", "");
        p0.setProperty("valintalaskentakoostepalvelu.app.password.to.valintaperusteet", "");
        p0.setProperty("valintalaskentakoostepalvelu.maxWorkerCount", "0");

        p0.setProperty("omatsivut.email.application.modify.link.en", "https://en.test.domain/token/");
        p0.setProperty("omatsivut.email.application.modify.link.fi", "https://fi.test.domain/token/");
        p0.setProperty("omatsivut.email.application.modify.link.sv", "https://sv.test.domain/token/");

        p0.setProperty("valintalaskentakoostepalvelu.tarjonta.sync.cron", "0 0 0 * * SUN-SAT");

        org.springframework.context.support.PropertySourcesPlaceholderConfigurer defaultProps = new org.springframework.context.support.PropertySourcesPlaceholderConfigurer();
        defaultProps.setProperties(p0);
        defaultProps.setOrder(0);
        defaultProps.setLocalOverride(true);
        return defaultProps;
    }

    private static final AbstractPhaseInterceptor<Message> INTERCEPTOR = new AbstractPhaseInterceptor<Message>(Phase.PRE_PROTOCOL) {
        @Override
        public void handleMessage(Message message) throws Fault {
        }

    };

    private static ApplicationSession APPLICATION_SESSION = new ApplicationSession(null, null, null, null, null, null, null) {
        @Override
        public CompletableFuture<SessionToken> getSessionToken() {
            return CompletableFuture.completedFuture(new SessionToken(
                    new ServiceTicket("http://localhost/service", "service-ticket"),
                    new HttpCookie("session", "session-uuid")
                    )
            );
        }
        @Override
        public void invalidateSession(SessionToken session) { }
    };

    @Bean(name = "AtaruApplicationSession")
    public ApplicationSession getAtaruApplicationSession() {
        return APPLICATION_SESSION;
    }

    @Bean(name = "HakuAppApplicationSession")
    public ApplicationSession getHakuAppApplicationSession() {
        return APPLICATION_SESSION;
    }

    @Bean(name = "ViestintapalveluApplicationSession")
    public ApplicationSession getViestintapalveluApplicationSession() {
        return APPLICATION_SESSION;
    }

    @Bean(name = "OppijanumerorekisteriApplicationSession")
    public ApplicationSession getOppijanumerorekisteriApplicationSession() {
        return APPLICATION_SESSION;
    }

    @Bean(name = "springSecurityFilterChain")
    public static Filter getFilter() {
        return new Filter() {
            @Override
            public void init(FilterConfig filterConfig) {

            }

            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
                filterChain.doFilter(servletRequest, servletResponse);
            }

            @Override
            public void destroy() {

            }
        };
    }

    @Bean(name = "viestintapalveluClientCasInterceptor")
    public AbstractPhaseInterceptor<Message> getViestintapalveluClientCasInterceptor() {
        return INTERCEPTOR;
    }

    @Bean(name = "ryhmasahkopostiClientCasInterceptor")
    public AbstractPhaseInterceptor<Message> getRyhmasahkopostiClientCasInterceptor() {
        return INTERCEPTOR;
    }

    @Bean(name = "AuthenticationServiceRestClientCasInterceptor")
    public AbstractPhaseInterceptor<Message> getAuthenticationServiceRestClientCasInterceptor() {
        return INTERCEPTOR;
    }

    @Bean(name = "OppijanumerorekisteriServiceRestClientCasInterceptor")
    public AbstractPhaseInterceptor<Message> getOppijanumerorekisteriServiceRestClientCasInterceptor() {
        return INTERCEPTOR;
    }

    @Bean(name = "HakemusServiceRestClientAsAdminCasInterceptor")
    public AbstractPhaseInterceptor<Message> getHakemusServiceRestClientAsAdminCasInterceptor() {
        return INTERCEPTOR;
    }

    @Bean(name = "adminDokumenttipalveluRestClientCasInterceptor")
    public AbstractPhaseInterceptor<Message> getAdminDokumenttipalveluRestClientCasInterceptor() {
        return INTERCEPTOR;
    }

    @Bean(name = "ValintalaskentaCasInterceptor")
    public AbstractPhaseInterceptor<Message> getValintalaskentaCasInterceptor() {
        return INTERCEPTOR;
    }

    @Bean(name = "SeurantaRestClientCasInterceptor")
    public AbstractPhaseInterceptor<Message> getSeurantaRestClientCasInterceptor() {
        return INTERCEPTOR;
    }

    @Bean(name = "SuoritusrekisteriRestClientCasInterceptor")
    public AbstractPhaseInterceptor<Message> getSuoritusrekisteriRestClientCasInterceptor() {
        return INTERCEPTOR;
    }

    @Bean(name = "koodiServiceCasInterceptor")
    public AbstractPhaseInterceptor<Message> getKoodiServiceCasInterceptor() {
        return INTERCEPTOR;
    }

    @Bean(name = "OrganisaatioResourceClientCasInterceptor")
    public AbstractPhaseInterceptor<Message> getOrganisaatioResourceClientCasInterceptor() {
        return INTERCEPTOR;
    }
}
