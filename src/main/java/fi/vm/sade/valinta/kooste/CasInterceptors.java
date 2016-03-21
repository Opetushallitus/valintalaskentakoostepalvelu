package fi.vm.sade.valinta.kooste;

import fi.vm.sade.authentication.cas.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("default")
@Configuration
public class CasInterceptors {

    @Bean(name = "sessionCache")
    public CasFriendlyCache getCasFriendlyCache() {
        return new CasFriendlyCache();
    }

    @Bean(name="viestintapalveluClientCasInterceptor")
    public fi.vm.sade.authentication.cas.CasFriendlyCxfInterceptor getViestintapalveluClientCasInterceptor(
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor("viestintapalveluClientCasInterceptor",appClientUsername,appClientPassword);
    }
    @Bean(name="ValintakoeRestClientCasInterceptor")
    public fi.vm.sade.authentication.cas.CasFriendlyCxfInterceptor getValintakoeRestClientCasInterceptor(
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor("ValintakoeRestClientCasInterceptor",appClientUsername,appClientPassword);
    }
    @Bean(name="HakemusServiceRestClientAsAdminCasInterceptor")
    public fi.vm.sade.authentication.cas.CasFriendlyCxfInterceptor getHakemusServiceRestClientAsAdminCasInterceptor(
            @Value("${valintalaskentakoostepalvelu.app.username.to.haku}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.haku}") String appClientPassword) {
        return createCasInterceptor("HakemusServiceRestClientAsAdminCasInterceptor",appClientUsername,appClientPassword);
    }
    @Bean(name="AuthenticationServiceRestClientCasInterceptor")
    public fi.vm.sade.authentication.cas.CasFriendlyCxfInterceptor getAuthenticationServiceRestClientCasInterceptor(
            @Value("${valintalaskentakoostepalvelu.app.username.to.haku}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.haku}") String appClientPassword) {
        return createCasInterceptor("AuthenticationServiceRestClientAsAdminCasInterceptor",appClientUsername,appClientPassword);
    }
    @Bean(name="adminDokumenttipalveluRestClientCasInterceptor")
     public fi.vm.sade.authentication.cas.CasFriendlyCxfInterceptor getAdminDokumenttipalveluRestClientCasInterceptor(
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintaperusteet}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintaperusteet}") String appClientPassword) {
        return createCasInterceptor("adminDokumenttipalveluRestClientCasInterceptor",appClientUsername,appClientPassword);
    }
    @Bean(name="sijoitteluTilaServiceRestClientCasInterceptor")
    public fi.vm.sade.authentication.cas.CasFriendlyCxfInterceptor getSijoitteluTilaServiceRestClientCasInterceptor(
            @Value("${valintalaskentakoostepalvelu.app.username.to.sijoittelu}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.sijoittelu}") String appClientPassword) {
        return createCasInterceptor("sijoitteluTilaServiceRestClientCasInterceptor",appClientUsername,appClientPassword);
    }
    @Bean(name="SijoitteluServiceRestClientCasInterceptor")
     public fi.vm.sade.authentication.cas.CasFriendlyCxfInterceptor getSijoitteluServiceRestClientCasInterceptor(
            @Value("${valintalaskentakoostepalvelu.app.username.to.sijoittelu}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.sijoittelu}") String appClientPassword) {
        return createCasInterceptor("SijoitteluServiceRestClientCasInterceptor",appClientUsername,appClientPassword);
    }
    @Bean(name="ValintalaskentaHakukohdeRestClientCasInterceptor")
    public fi.vm.sade.authentication.cas.CasFriendlyCxfInterceptor getValintalaskentaHakukohdeRestClientCasInterceptor(
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor("ValintalaskentaHakukohdeRestClientCasInterceptor",appClientUsername,appClientPassword);
    }
    @Bean(name="SijoittelunSeurantaRestClientCasInterceptor")
    public fi.vm.sade.authentication.cas.CasFriendlyCxfInterceptor getSijoittelunSeurantaRestClientCasInterceptor(
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor("SijoittelunSeurantaRestClientCasInterceptor",appClientUsername,appClientPassword);
    }
    @Bean(name="ValintatietoRestClientCasInterceptor")
    public fi.vm.sade.authentication.cas.CasFriendlyCxfInterceptor getValintatietoRestClientCasInterceptor(
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor("ValintatietoRestClientCasInterceptor",appClientUsername,appClientPassword);
    }
    @Bean(name="koodiServiceCasInterceptor")
    public fi.vm.sade.authentication.cas.CasFriendlyCxfInterceptor getKoodiServiceCasInterceptor(
            @Value("${valintalaskentakoostepalvelu.app.username.to.koodisto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.koodisto}") String appClientPassword) {
        return createCasInterceptor("koodiServiceCasInterceptor",appClientUsername,appClientPassword);
    }
    @Bean(name="OrganisaatioResourceClientCasInterceptor")
    public fi.vm.sade.authentication.cas.CasFriendlyCxfInterceptor getOrganisaatioResourceClientCasInterceptor(
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword) {
        return createCasInterceptor("OrganisaatioResourceClientCasInterceptor",appClientUsername,appClientPassword);
    }

    private fi.vm.sade.authentication.cas.CasFriendlyCxfInterceptor createCasInterceptor(
            String callerServiceId,String appClientUsername,String appClientPassword) {
        CasFriendlyCxfInterceptor cas = new CasFriendlyCxfInterceptor();
        cas.setCache(new CasFriendlyCache());
        cas.setAppClientUsername(appClientUsername);
        cas.setAppClientPassword(appClientPassword);
        cas.setCallerService(callerServiceId);
        return cas;
    }

}
