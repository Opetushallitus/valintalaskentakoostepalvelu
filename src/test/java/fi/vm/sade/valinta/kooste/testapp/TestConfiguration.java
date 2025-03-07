package fi.vm.sade.valinta.kooste.testapp;

import fi.vm.sade.javautils.opintopolku_spring_security.Authorizer;
import fi.vm.sade.javautils.opintopolku_spring_security.OidProvider;
import fi.vm.sade.javautils.opintopolku_spring_security.OrganisationHierarchyAuthorizer;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.HarkinnanvaraisuusAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.impl.HarkinnanvaraisuusAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RyhmasahkopostiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.hakemukset.resource.HakemuksetResource;
import fi.vm.sade.valinta.kooste.hakemukset.service.ValinnanvaiheenValintakoekutsutService;
import fi.vm.sade.valinta.kooste.pistesyotto.resource.PistesyottoResource;
import fi.vm.sade.valinta.kooste.pistesyotto.service.PistesyottoExternalTuontiService;
import fi.vm.sade.valinta.kooste.pistesyotto.service.PistesyottoKoosteService;
import fi.vm.sade.valinta.kooste.pistesyotto.service.PistesyottoTuontiService;
import fi.vm.sade.valinta.kooste.pistesyotto.service.PistesyottoVientiService;
import fi.vm.sade.valinta.kooste.proxy.resource.suoritukset.OppijanSuorituksetProxyResource;
import fi.vm.sade.valinta.kooste.proxy.resource.viestintapalvelu.ViestintapalveluProxyResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.util.HakemuksetConverterUtil;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.SijoittelunTulosExcelKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.resource.ValintalaskentaExcelResource;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.service.ValintakoekutsutExcelService;
import fi.vm.sade.valinta.kooste.valintaperusteet.ValintaperusteetResource;
import fi.vm.sade.valinta.kooste.valintaperusteet.ValintaperusteetResourceV2;
import fi.vm.sade.valinta.kooste.valintatapajono.route.ValintatapajonoVientiRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.EPostiService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeetService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.KoekutsukirjeetService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.KirjeetHakukohdeCache;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.KoekutsukirjeetImpl;
import fi.vm.sade.valinta.kooste.viestintapalvelu.service.OsoitetarratService;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

@Profile("mockresources")
@Configuration
@ComponentScan(
    basePackages = {
      "fi.vm.sade.valinta.kooste.mocks",
      "fi.vm.sade.valinta.kooste.erillishaku",
      "fi.vm.sade.valinta.kooste.proxy.resource.erillishaku",
      "fi.vm.sade.valinta.kooste.valintatapajono.resource",
      "fi.vm.sade.valinta.kooste.valintatapajono.service",
      "fi.vm.sade.valinta.kooste.sijoittelu.komponentti",
      "fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti",
      "fi.vm.sade.valinta.kooste.hakemus.komponentti",
      "fi.vm.sade.valinta.kooste.viestintapalvelu.resource",
      "fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice",
      "fi.vm.sade.valinta.kooste.url"
    },
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          classes = {})
    })
public class TestConfiguration {

  @Bean
  public KoekutsukirjeetService getKoekutsuKirjeet(ApplicationContext ctx) {
    return ctx.getAutowireCapableBeanFactory().createBean(KoekutsukirjeetImpl.class);
  }

  @Bean
  public SijoittelunTulosExcelKomponentti getSijoittelunTulosExcelKomponentti(
      ApplicationContext ctx) {
    return ctx.getAutowireCapableBeanFactory().createBean(SijoittelunTulosExcelKomponentti.class);
  }

  @Bean
  public PistesyottoResource getPistesyottoResource(ApplicationContext ctx) {
    return ctx.getAutowireCapableBeanFactory().createBean(PistesyottoResource.class);
  }

  @Bean
  public PistesyottoExternalTuontiService getPistesyottoExternalTuontiService(
      ApplicationContext ctx) {
    return ctx.getAutowireCapableBeanFactory().createBean(PistesyottoExternalTuontiService.class);
  }

  @Bean
  public PistesyottoTuontiService getPistesyottoTuontiService(ApplicationContext ctx) {
    return ctx.getAutowireCapableBeanFactory().createBean(PistesyottoTuontiService.class);
  }

  @Bean
  public PistesyottoVientiService getPistesyottoVientiService(ApplicationContext ctx) {
    return ctx.getAutowireCapableBeanFactory().createBean(PistesyottoVientiService.class);
  }

  @Bean
  public PistesyottoKoosteService getPistesyottoKoosteService(ApplicationContext ctx) {
    return ctx.getAutowireCapableBeanFactory().createBean(PistesyottoKoosteService.class);
  }

  @Bean
  public ValintalaskentaExcelResource getValintalaskentaExcelResource(ApplicationContext ctx) {
    return ctx.getAutowireCapableBeanFactory().createBean(ValintalaskentaExcelResource.class);
  }

  @Bean
  public ValintakoekutsutExcelService getValintakoekutsutExcelService(ApplicationContext ctx) {
    return ctx.getAutowireCapableBeanFactory().createBean(ValintakoekutsutExcelService.class);
  }

  @Bean
  public OsoitetarratService getOsoitetarratService(ApplicationContext ctx) {
    return ctx.getAutowireCapableBeanFactory().createBean(OsoitetarratService.class);
  }

  @Bean
  public OppijanSuorituksetProxyResource getOppijanSuorituksetProxyResource(
      ApplicationContext ctx) {
    return ctx.getAutowireCapableBeanFactory().createBean(OppijanSuorituksetProxyResource.class);
  }

  @Bean
  public ViestintapalveluProxyResource getViestintapalveluProxyResource(ApplicationContext ctx) {
    return ctx.getAutowireCapableBeanFactory().createBean(ViestintapalveluProxyResource.class);
  }

  @Bean
  public HakemuksetResource getHakemuksetResource(ApplicationContext ctx) {
    return ctx.getAutowireCapableBeanFactory().createBean(HakemuksetResource.class);
  }

  @Bean
  public ValinnanvaiheenValintakoekutsutService getValinnanvaiheenValintakoekutsutService(
      ApplicationContext ctx) {
    return ctx.getAutowireCapableBeanFactory()
        .createBean(ValinnanvaiheenValintakoekutsutService.class);
  }

  @Bean
  public HarkinnanvaraisuusAsyncResource getHarkinnanvaraisuusAsyncResource(
      ApplicationContext ctx) {
    return ctx.getAutowireCapableBeanFactory()
        .createBean(HarkinnanvaraisuusAsyncResourceImpl.class);
  }

  @Bean
  public OidProvider getOidProvider() {
    return new OidProvider(
        "http://localhost",
        "1.2.246.562.10.00000000001",
        "1.2.246.562.10.00000000001.koostepalvelun-testit");
  }

  @Bean
  public ValintaperusteetResource getValintaperusteetResource(ApplicationContext ctx) {
    return ctx.getAutowireCapableBeanFactory().createBean(ValintaperusteetResource.class);
  }

  @Bean
  public ValintaperusteetResourceV2 getValintaperusteetResourceV2(ApplicationContext ctx) {
    return ctx.getAutowireCapableBeanFactory().createBean(ValintaperusteetResourceV2.class);
  }

  @Bean
  public HakemuksetConverterUtil getHakemuksetConverterUtil(ApplicationContext ctx) {
    return ctx.getAutowireCapableBeanFactory().createBean(HakemuksetConverterUtil.class);
  }

  @Bean
  public ValintatapajonoVientiRoute getValintatapajonoVientiRoute() {
    return Mockito.mock(ValintatapajonoVientiRoute.class);
  }

  @Bean
  public EPostiService getEPostiService() {
    return Mockito.mock(EPostiService.class);
  }

  @Bean
  public HyvaksymiskirjeetService getHyvaksymiskirjeetService() {
    return Mockito.mock(HyvaksymiskirjeetService.class);
  }

  @Bean
  public Authorizer getAuthorizer() {
    return Mockito.mock(Authorizer.class);
  }

  @Bean
  public ValintapisteAsyncResource getValintapisteAsyncResource() {
    return Mockito.mock(ValintapisteAsyncResource.class);
  }

  @Bean
  public ViestintapalveluAsyncResource getViestintapalveluAsyncResource() {
    return Mockito.mock(ViestintapalveluAsyncResource.class);
  }

  @Bean
  public RyhmasahkopostiAsyncResource getRyhmasahkopostiAsyncResource() {
    return Mockito.mock(RyhmasahkopostiAsyncResource.class);
  }

  @Bean
  public KirjeetHakukohdeCache getKirjeetHakukohdeCache() {
    return Mockito.mock(KirjeetHakukohdeCache.class);
  }

  @Bean
  public KoodistoAsyncResource getKoodistoAsyncResource() {
    return Mockito.mock(KoodistoAsyncResource.class);
  }

  @Bean
  public OrganisationHierarchyAuthorizer getOrganisationHierarchyAuthorizer() {
    return Mockito.mock(OrganisationHierarchyAuthorizer.class);
  }
}
