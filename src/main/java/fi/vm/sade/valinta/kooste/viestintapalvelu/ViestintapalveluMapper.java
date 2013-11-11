package fi.vm.sade.valinta.kooste.viestintapalvelu;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Ei ota mukaan null arvoja!
 */
@Provider
public class ViestintapalveluMapper implements ContextResolver<ObjectMapper> {

    private final ObjectMapper objectMapper;

    public ViestintapalveluMapper() {
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationConfig(objectMapper.getSerializationConfig().withSerializationInclusion(
                Inclusion.NON_NULL));
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }
}
