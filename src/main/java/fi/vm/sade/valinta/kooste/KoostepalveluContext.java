package fi.vm.sade.valinta.kooste;

import fi.vm.sade.javautils.opintopolku_spring_security.Authorizer;
import fi.vm.sade.javautils.opintopolku_spring_security.OidProvider;
import fi.vm.sade.javautils.opintopolku_spring_security.OrganisationHierarchyAuthorizer;
import fi.vm.sade.javautils.opintopolku_spring_security.ThreadLocalAuthorizer;
import fi.vm.sade.valinta.kooste.external.resource.HttpClients;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({KelaRouteConfig.class, JaxrsConfiguration.class})
public class KoostepalveluContext {
  static final String TYHJA_ARVO_POIKKEUS = "Reititysta ei voida jatkaa tyhjalle arvolle!";

  private static final Logger LOG = LoggerFactory.getLogger(KoostepalveluContext.class);

  @Bean
  public OidProvider getOidProvider(
      @Value("${valintalaskentakoostepalvelu.organisaatio-service-url}")
          String organisaatioServiceUrl,
      @Value("${root.organisaatio.oid}") String rootOrganisationOid) {
    return new OidProvider(organisaatioServiceUrl, rootOrganisationOid, HttpClients.CALLER_ID);
  }

  @Bean
  public OrganisationHierarchyAuthorizer getOrganisationHierarchyAuthorizer() {
    return new OrganisationHierarchyAuthorizer();
  }

  @Bean(name = "authorizer")
  public Authorizer getAuthorizerImpl(
      OrganisationHierarchyAuthorizer organisationHierarchyAuthorizer) {
    return new ThreadLocalAuthorizer();
  }

}
