package fi.vm.sade.valinta.kooste;

import org.jasig.cas.client.authentication.AttributePrincipalImpl;
import org.jasig.cas.client.validation.AssertionImpl;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

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
    private static CasAuthenticationToken casAuthenticationToken = null;

    public static void clear() {
        casAuthenticationToken = null;
    }

    public static void setRolesToReturnInFakeAuthentication(String... roles) {
        String key = MockOpintopolkuCasAuthenticationFilter.class.getSimpleName() + "-key";
        String principal = "1.2.246.562.24.99999999999";
        String credentials = MockOpintopolkuCasAuthenticationFilter.class.getSimpleName() + "-creds";
        casAuthenticationToken = new CasAuthenticationToken(key, principal, credentials,
            Stream.of(roles).map(SimpleGrantedAuthority::new).collect(Collectors.toList()),
            new User(MockOpintopolkuCasAuthenticationFilter.class.getSimpleName().toLowerCase(), "salasana", Collections.singletonList(new SimpleGrantedAuthority("rooli"))),
            new AssertionImpl(new AttributePrincipalImpl(MockOpintopolkuCasAuthenticationFilter.class.getSimpleName())));

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (casAuthenticationToken == null) {
            throw new RuntimeException("CAS authentication mock not set");
        }
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(casAuthenticationToken);
        chain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }
}
