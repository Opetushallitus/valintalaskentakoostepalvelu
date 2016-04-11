package fi.vm.sade.valinta.kooste;

import fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("default")
@Configuration
public class CasInterceptors {

    @Bean(name="viestintapalveluClientCasInterceptor")
    public fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor getViestintapalveluClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.viestintapalvelu}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="ValintakoeRestClientCasInterceptor")
    public fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor getValintakoeRestClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.valintalaskenta-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="HakemusServiceRestClientAsAdminCasInterceptor")
    public fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor getHakemusServiceRestClientAsAdminCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.haku-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.haku}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.haku}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="AuthenticationServiceRestClientCasInterceptor")
    public fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor getAuthenticationServiceRestClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.haku-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.haku}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.haku}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="adminDokumenttipalveluRestClientCasInterceptor")
     public fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor getAdminDokumenttipalveluRestClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.dokumenttipalvelu}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintaperusteet}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintaperusteet}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="sijoitteluTilaServiceRestClientCasInterceptor")
    public fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor getSijoitteluTilaServiceRestClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.sijoittelu-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.sijoittelu}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.sijoittelu}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="SijoitteluServiceRestClientCasInterceptor")
     public fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor getSijoitteluServiceRestClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.sijoittelu-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.sijoittelu}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.sijoittelu}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="ValintalaskentaHakukohdeRestClientCasInterceptor")
    public fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor getValintalaskentaHakukohdeRestClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.valintalaskenta-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="SijoittelunSeurantaRestClientCasInterceptor")
    public fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor getSijoittelunSeurantaRestClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.seuranta}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="ValintatietoRestClientCasInterceptor")
    public fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor getValintatietoRestClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.valintalaskenta-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="koodiServiceCasInterceptor")
    public fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor getKoodiServiceCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.koodisto-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.koodisto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.koodisto}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }
    @Bean(name="OrganisaatioResourceClientCasInterceptor")
    public fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor getOrganisaatioResourceClientCasInterceptor(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.organisaatio-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor(webCasUrl,targetService,appClientUsername,appClientPassword);
    }

    private fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor createCasInterceptor(
            String webCasUrl,String targetService,String appClientUsername,String appClientPassword) {
        CasApplicationAsAUserInterceptor cas = new CasApplicationAsAUserInterceptor();
        cas.setWebCasUrl(webCasUrl);
        cas.setTargetService(targetService);
        cas.setAppClientUsername(appClientUsername);
        cas.setAppClientPassword(appClientPassword);
        return cas;
    }

}
