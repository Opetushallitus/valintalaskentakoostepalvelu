package fi.vm.sade.valinta.kooste.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;

@Profile("default")
@Configuration
@ImportResource("/spring/application-context-production.xml")
public class ProductionConfiguration {}
