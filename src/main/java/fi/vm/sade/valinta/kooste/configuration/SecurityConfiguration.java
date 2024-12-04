package fi.vm.sade.valinta.kooste.configuration;

import fi.vm.sade.java_utils.security.OpintopolkuCasAuthenticationFilter;
import fi.vm.sade.javautils.kayttooikeusclient.OphUserDetailsServiceImpl;
import org.apereo.cas.client.session.SingleSignOutFilter;
import org.apereo.cas.client.validation.Cas20ProxyTicketValidator;
import org.apereo.cas.client.validation.TicketValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Profile({"default", "dev"})
@Configuration
@Order(2)
@EnableMethodSecurity(securedEnabled = true)
@EnableWebSecurity
public class SecurityConfiguration {
  private Environment environment;

  private String service;
  private Boolean sendRenew;
  private String key;

  @Autowired
  public SecurityConfiguration(
      final Environment environment,
      @Value("${cas.service.valintalaskentakoostepalvelu}") String service,
      @Value("${cas.service.key}") String key,
      @Value("${cas.sendRenew}") boolean sendRenew) {
    this.environment = environment;

    this.service = service;
    this.key = key;
    this.sendRenew = sendRenew;
  }

  @Bean
  public ServiceProperties serviceProperties() {
    ServiceProperties serviceProperties = new ServiceProperties();
    serviceProperties.setService(this.service + "/j_spring_cas_security_check");
    serviceProperties.setSendRenew(this.sendRenew);
    serviceProperties.setAuthenticateAllArtifacts(true);
    return serviceProperties;
  }

  //
  // CAS authentication provider (authentication manager)
  //

  @Bean
  public CasAuthenticationProvider casAuthenticationProvider() {
    final String host =
        environment.getProperty("host.alb", environment.getRequiredProperty("host.virkailija"));
    CasAuthenticationProvider casAuthenticationProvider = new CasAuthenticationProvider();
    casAuthenticationProvider.setUserDetailsService(
        new OphUserDetailsServiceImpl(
            host, "1.2.246.562.10.00000000001.valintalaskentakoostepalvelu"));
    casAuthenticationProvider.setServiceProperties(serviceProperties());
    casAuthenticationProvider.setTicketValidator(ticketValidator());
    casAuthenticationProvider.setKey(this.key);
    return casAuthenticationProvider;
  }

  @Bean
  public TicketValidator ticketValidator() {
    Cas20ProxyTicketValidator ticketValidator =
        new Cas20ProxyTicketValidator(environment.getRequiredProperty("web.url.cas"));
    ticketValidator.setAcceptAnyProxy(true);
    return ticketValidator;
  }

  //
  // CAS filter
  //

  @Bean
  public CasAuthenticationFilter casAuthenticationFilter(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    OpintopolkuCasAuthenticationFilter casAuthenticationFilter =
        new OpintopolkuCasAuthenticationFilter(serviceProperties());
    casAuthenticationFilter.setAuthenticationManager(
        authenticationConfiguration.getAuthenticationManager());
    casAuthenticationFilter.setFilterProcessesUrl("/j_spring_cas_security_check");
    return casAuthenticationFilter;
  }

  //
  // CAS single logout filter
  // requestSingleLogoutFilter is not configured because our users always sign out through CAS
  // logout (using virkailija-raamit
  // logout button) when CAS calls this filter if user has ticket to this service.
  //
  @Bean
  public SingleSignOutFilter singleSignOutFilter() {
    SingleSignOutFilter singleSignOutFilter = new SingleSignOutFilter();
    singleSignOutFilter.setIgnoreInitConfiguration(true);
    return singleSignOutFilter;
  }

  //
  // CAS entry point
  //

  @Bean
  public CasAuthenticationEntryPoint casAuthenticationEntryPoint() {
    CasAuthenticationEntryPoint casAuthenticationEntryPoint = new CasAuthenticationEntryPoint();
    casAuthenticationEntryPoint.setLoginUrl(
        environment.getRequiredProperty("web.url.cas") + "/login");
    casAuthenticationEntryPoint.setServiceProperties(serviceProperties());
    return casAuthenticationEntryPoint;
  }

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http, CasAuthenticationFilter casAuthenticationFilter) throws Exception {

    HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
    requestCache.setMatchingRequestParameterName(null);

    http.headers(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            (authorizeHttpRequests) ->
                authorizeHttpRequests
                    .requestMatchers(new RegexRequestMatcher("^/?$", "GET"))
                    .permitAll()
                    .requestMatchers(new RegexRequestMatcher("^/buildversion.txt$", "GET"))
                    .permitAll()
                    .requestMatchers(new RegexRequestMatcher("^/swagger-ui(/.*)?", "GET"))
                    .permitAll()
                    .requestMatchers(new RegexRequestMatcher("^/swagger(/.*)?", "GET"))
                    .permitAll()
                    .requestMatchers(new RegexRequestMatcher("^/v3/api-docs(/.*)?", "GET"))
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilter(casAuthenticationFilter)
        .exceptionHandling(eh -> eh.authenticationEntryPoint(casAuthenticationEntryPoint()))
        .addFilterBefore(singleSignOutFilter(), CasAuthenticationFilter.class);

    return http.build();
  }
}
