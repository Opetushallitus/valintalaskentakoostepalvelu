package fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fi.vm.sade.valinta.kooste.external.resource.suorituspalvelu.SuorituspalveluAsyncResource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Testaa HarkinnanvaraisuusResourcen @PreAuthorize-oikeustarkistukset. MockServicesApp-ympäristössä
 * metoditason tietoturva ei ole käytössä, joten tarkistukset testataan tässä erikseen.
 */
@SpringJUnitConfig(HarkinnanvaraisuusResourceAuthorizationTest.Config.class)
public class HarkinnanvaraisuusResourceAuthorizationTest {
  private static final String HAKEMUS_OID = "1.2.246.562.11.00000000001";

  @Configuration
  @EnableMethodSecurity
  static class Config {
    @Bean
    HarkinnanvaraisuusResource harkinnanvaraisuusResource() {
      return new HarkinnanvaraisuusResource();
    }

    @Bean
    SuorituspalveluAsyncResource suorituspalveluAsyncResource() {
      return mock(SuorituspalveluAsyncResource.class);
    }
  }

  @Autowired private HarkinnanvaraisuusResource resource;
  @Autowired private SuorituspalveluAsyncResource suorituspalveluAsyncResource;

  @BeforeEach
  public void setUp() {
    reset(suorituspalveluAsyncResource);
    when(suorituspalveluAsyncResource.haeHarkinnanvaraisuudet(List.of(HAKEMUS_OID)))
        .thenReturn(CompletableFuture.completedFuture(List.of()));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void hakemuksilleEstetaanIlmanHakemusoikeutta() {
    kirjauduRoolilla("ROLE_APP_VALINTOJENTOTEUTTAMINEN_CRUD");

    assertThrows(
        AccessDeniedException.class,
        () ->
            resource.hakemustenHarkinnanvaraisuustiedot(
                List.of(HAKEMUS_OID), new MockHttpServletRequest()));
    verifyNoInteractions(suorituspalveluAsyncResource);
  }

  @Test
  public void hakemuksilleSallitaanHakemusoikeudella() {
    kirjauduRoolilla("ROLE_APP_HAKEMUS_READ");

    resource.hakemustenHarkinnanvaraisuustiedot(List.of(HAKEMUS_OID), new MockHttpServletRequest());

    verify(suorituspalveluAsyncResource).haeHarkinnanvaraisuudet(List.of(HAKEMUS_OID));
  }

  private static void kirjauduRoolilla(String rooli) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "1.2.246.562.24.99999999999", "", List.of(new SimpleGrantedAuthority(rooli))));
  }
}
