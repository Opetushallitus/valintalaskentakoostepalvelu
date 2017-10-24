package fi.vm.sade.valinta.kooste;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.ValintaTulosServiceProxyResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JaxrsConfiguration {

    @Bean(name = "jsonProvider")
    public JacksonJsonProvider getJacksonJsonProvider() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new ValintaTulosServiceProxyResource.ValintaTulosServiceSerializersModule())
                .registerModule(new JavaTimeModule())
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module());
        return new JacksonJsonProvider(objectMapper);
    }
}
