package fi.vm.sade.valinta.kooste.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.tarjonta.api.OrganisaatioResource;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import javax.ws.rs.ForbiddenException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.GrantedAuthority;

public class AuthorityCheckServiceTest {

  @InjectMocks private AuthorityCheckService authorityCheckService;

  @Mock private OrganisaatioResource organisaatioResource;

  @Mock private TarjontaAsyncResource tarjontaAsyncResource;

  @Before
  public void initMocks() throws Exception {
    MockitoAnnotations.initMocks(this);
    Mockito.when(organisaatioResource.parentoids("oid.1")).thenReturn("parent.oid.1/oid.1");
    Mockito.when(organisaatioResource.parentoids("oid.2")).thenReturn("parent.oid.2/oid.2");
    Mockito.when(organisaatioResource.parentoids("oid.3")).thenReturn("parent.oid.3/oid.3");
    Mockito.when(tarjontaAsyncResource.haeHaku("haku.oid"))
        .thenReturn(CompletableFuture.completedFuture(new HakuV1RDTO()));
  }

  private class TestAuthority implements GrantedAuthority {

    public TestAuthority(String role) {
      this.role = role;
    }

    private String role;

    public String getAuthority() {
      return role;
    }
  }

  @Test
  public void testIsAuthorizedForAnyParentOid() {
    String[] organisaatioOids = {"oid.1", "oid.2", "oid.3"};
    Collection<? extends GrantedAuthority> userRoles =
        Collections.singleton(new TestAuthority("authorized_parent.oid.3"));
    Collection<String> requiredRoles = Collections.singleton("authorized");

    boolean authorized =
        authorityCheckService.isAuthorizedForAnyParentOid(
            organisaatioOids, userRoles, requiredRoles);
    assertTrue(authorized);

    String[] organisaatioOids2 = {"oid.1", "oid.2"};

    boolean authorized2 =
        authorityCheckService.isAuthorizedForAnyParentOid(
            organisaatioOids2, userRoles, requiredRoles);
    assertFalse(authorized2);
  }

  @Test(expected = ForbiddenException.class)
  public void testCheckAuthorizationForHaku() {
    authorityCheckService.checkAuthorizationForHaku("haku.oid", Collections.EMPTY_SET);
  }
}
