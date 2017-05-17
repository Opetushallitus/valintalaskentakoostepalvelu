package fi.vm.sade.valinta.kooste;

import fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor;
import fi.vm.sade.valinta.kooste.cas.CasKoosteInterceptor;
import fi.vm.sade.valinta.kooste.cas.ComparingTicketCachePolicy;
import fi.vm.sade.valinta.kooste.cas.ConcurrentTicketCachePolicy;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("default")
@Configuration
public class CasInterceptors {

    @Bean(name="viestintapalveluClientCasInterceptor")
    public AbstractPhaseInterceptor<Message> getViestintapalveluClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.viestintapalvelu}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="ryhmasahkopostiClientCasInterceptor")
    public AbstractPhaseInterceptor<Message> getRyhmasahkopostiClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.ryhmasahkoposti-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="ValintakoeRestClientCasInterceptor")
    public AbstractPhaseInterceptor<Message> getValintakoeRestClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.valintalaskenta-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="HakemusServiceRestClientAsAdminCasInterceptor")
    public AbstractPhaseInterceptor<Message> getHakemusServiceRestClientAsAdminCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.haku-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.haku}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.haku}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="AuthenticationServiceRestClientCasInterceptor")
    public AbstractPhaseInterceptor<Message> getAuthenticationServiceRestClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.authentication-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.haku}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.haku}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="adminDokumenttipalveluRestClientCasInterceptor")
     public AbstractPhaseInterceptor<Message> getAdminDokumenttipalveluRestClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.dokumenttipalvelu}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintaperusteet}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintaperusteet}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="sijoitteluTilaServiceRestClientCasInterceptor")
    public AbstractPhaseInterceptor<Message> getSijoitteluTilaServiceRestClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.sijoittelu-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.sijoittelu}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.sijoittelu}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="SijoitteluServiceRestClientCasInterceptor")
     public AbstractPhaseInterceptor<Message> getSijoitteluServiceRestClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.sijoittelu-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.sijoittelu}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.sijoittelu}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="ValintalaskentaHakukohdeRestClientCasInterceptor")
    public AbstractPhaseInterceptor<Message> getValintalaskentaHakukohdeRestClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.valintalaskenta-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="SijoittelunSeurantaRestClientCasInterceptor")
    public AbstractPhaseInterceptor<Message> getSijoittelunSeurantaRestClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.seuranta}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="ValintatietoRestClientCasInterceptor")
    public AbstractPhaseInterceptor<Message> getValintatietoRestClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.valintalaskenta-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="SeurantaRestClientCasInterceptor")
    public AbstractPhaseInterceptor<Message> getSeurantaRestClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.seuranta}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="SuoritusrekisteriRestClientCasInterceptor")
    public AbstractPhaseInterceptor<Message> getSuoritusrekisteriRestClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("https://${host.virkailija}/suoritusrekisteri/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="koodiServiceCasInterceptor")
    public AbstractPhaseInterceptor<Message> getKoodiServiceCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.koodisto-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.koodisto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.koodisto}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="OrganisaatioResourceClientCasInterceptor")
    public AbstractPhaseInterceptor<Message> getOrganisaatioResourceClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.organisaatio-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }

    private AbstractPhaseInterceptor<Message> createCasInterceptor(
            String webCasUrl,String targetService,String appClientUsername,String appClientPassword) {
        return new CasKoosteInterceptor(webCasUrl, targetService, appClientUsername, appClientPassword);
    }

}
