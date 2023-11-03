package fi.vm.sade.valinta.kooste.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SwaggerConfiguration implements WebMvcConfigurer {
  @Bean
  public OpenAPI valintalaskentakoostepalveluAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Valintalaskentakoostepalvelu API")
                .description("Valintalaskentakoostepalvelu")
                .version("v1.0"));
  }

  @Override
  public void addViewControllers(final ViewControllerRegistry registry) {
    registry.addRedirectViewController("/swagger/**", "/swagger-ui/index.html");
    registry.addRedirectViewController("/swagger", "/swagger-ui/index.html");
  }
}
