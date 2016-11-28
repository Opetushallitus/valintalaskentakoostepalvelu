package fi.vm.sade.valinta.kooste;

import org.jasig.cas.client.authentication.AttributePrincipalImpl;
import org.jasig.cas.client.validation.AssertionImpl;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.ldap.userdetails.LdapAuthority;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MockOpintopolkuCasAuthenticationFilter implements Filter {
    private static CasAuthenticationToken CAS_AUTHENTICATION_TOKEN;

    static {
        MockOpintopolkuCasAuthenticationFilter.setRolesToReturnInFakeAuthentication("ROLE_APP_HAKEMUS_READ_UPDATE_1.2.246.562.10.00000000001");
    }

    public static void setRolesToReturnInFakeAuthentication(String... roles) {
        String key = MockOpintopolkuCasAuthenticationFilter.class.getSimpleName() + "-key";
        String principal = MockOpintopolkuCasAuthenticationFilter.class.getSimpleName() + "-principal";
        String credentials = MockOpintopolkuCasAuthenticationFilter.class.getSimpleName() + "-creds";
        CAS_AUTHENTICATION_TOKEN = new CasAuthenticationToken(key, principal, credentials,
            Stream.of(roles).map(SimpleGrantedAuthority::new).collect(Collectors.toList()),
            new User(MockOpintopolkuCasAuthenticationFilter.class.getSimpleName().toLowerCase(), "salasana", Collections.singletonList(new LdapAuthority("rooli", "dn"))),
            new AssertionImpl(new AttributePrincipalImpl(MockOpintopolkuCasAuthenticationFilter.class.getSimpleName())));
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(CAS_AUTHENTICATION_TOKEN);
        chain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
