package fi.vm.sade.valinta.kooste.testapp;

import fi.vm.sade.valinta.sharedutils.FakeAuthenticationInitialiser;
import jakarta.servlet.*;
import java.io.IOException;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@Profile("mockresources")
public class TestAuthFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    FakeAuthenticationInitialiser.fakeAuthentication();
    chain.doFilter(request, response);
  }
}
