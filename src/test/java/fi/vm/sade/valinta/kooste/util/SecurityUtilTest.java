package fi.vm.sade.valinta.kooste.util;

import static fi.vm.sade.valinta.kooste.util.SecurityUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

public class SecurityUtilTest {

  private static final String ORGANIZATION_OID_PREFIX = "1.2.246.562.10";
  private static final String ORGANIZATION_GROUP_OID_PREFIX = "1.2.246.562.28";

  @Test
  public void TestIsRootOrganizationOID() {
    boolean result1 = isRootOrganizationOID("organisaatio.oid");
    boolean result2 = isRootOrganizationOID("1.2.246.562.10.00000000001");

    assertFalse(result1);
    assertTrue(result2);
  }

  @Test
  public void TestParseOrganizationOidFromSecurityRole() {
    Optional<String> result1 =
        parseOrganizationOidFromSecurityRole("auth_crud_ei.organisaatio.oid");
    Optional<String> result2 =
        parseOrganizationOidFromSecurityRole("auth_crud_" + ORGANIZATION_OID_PREFIX + "111.222");

    assertFalse(result1.isPresent());
    assertTrue(result2.isPresent());
    assertEquals(ORGANIZATION_OID_PREFIX + "111.222", result2.get());
  }

  @Test
  public void TestParseOrganizationGroupOidFromSecurityRole() {
    Optional<String> result1 =
        parseOrganizationGroupOidFromSecurityRole("auth_crud_ei.organisaatio.group.oid");
    Optional<String> result2 =
        parseOrganizationGroupOidFromSecurityRole(
            "auth_crud_" + ORGANIZATION_GROUP_OID_PREFIX + "222.333");

    assertFalse(result1.isPresent());
    assertTrue(result2.isPresent());
    assertEquals(ORGANIZATION_GROUP_OID_PREFIX + "222.333", result2.get());
  }

  @Test
  public void TestParseOrganizationOidsFromSecurityRoles() {
    Set<String> oids =
        Stream.of(ORGANIZATION_OID_PREFIX + "222.333", ORGANIZATION_OID_PREFIX + "111.222")
            .collect(Collectors.toSet());

    Set<String> result =
        parseOrganizationOidsFromSecurityRoles(
            Arrays.asList(
                "auth_crud_" + ORGANIZATION_OID_PREFIX + "222.333",
                "auth_crud_" + ORGANIZATION_OID_PREFIX + "111.222",
                "auth_crud_" + ORGANIZATION_GROUP_OID_PREFIX + "444.555"));

    assertEquals(oids, result);
  }

  @Test
  public void TestParseOrganizationGroupOidsFromSecurityRoles() {
    Set<String> oids =
        Stream.of(
                ORGANIZATION_GROUP_OID_PREFIX + "111.222",
                ORGANIZATION_GROUP_OID_PREFIX + "444.555")
            .collect(Collectors.toSet());

    Set<String> result =
        parseOrganizationGroupOidsFromSecurityRoles(
            Arrays.asList(
                "auth_crud_" + ORGANIZATION_OID_PREFIX + "222.333",
                "auth_crud_" + ORGANIZATION_GROUP_OID_PREFIX + "111.222",
                "auth_crud_" + ORGANIZATION_GROUP_OID_PREFIX + "444.555"));

    assertEquals(oids, result);
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
  public void TestContainsOphRole() {
    Set<GrantedAuthority> oids1 =
        Stream.of(
                "auth_crud_" + ORGANIZATION_GROUP_OID_PREFIX + "111.222",
                "auth_crud_" + ORGANIZATION_GROUP_OID_PREFIX + "444.555")
            .map(TestAuthority::new)
            .collect(Collectors.toSet());
    Set<GrantedAuthority> oids2 =
        Stream.of(
                "auth_crud_" + ORGANIZATION_GROUP_OID_PREFIX + "111.222",
                "auth_crud_" + ORGANIZATION_GROUP_OID_PREFIX + "444.555",
                "1.2.246.562.10.00000000001")
            .map(TestAuthority::new)
            .collect(Collectors.toSet());

    boolean result1 = containsOphRole(oids1);
    boolean result2 = containsOphRole(oids2);

    assertFalse(result1);
    assertTrue(result2);
  }
}
