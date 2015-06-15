package fi.vm.sade.valinta.kooste.swagger;

import com.wordnik.swagger.core.filter.SwaggerSpecFilter;
import com.wordnik.swagger.model.ApiDescription;
import com.wordnik.swagger.model.Operation;
import com.wordnik.swagger.model.Parameter;

import java.util.List;
import java.util.Map;

/**
 *         Ohittaa asyncresponse olion body parametrina
 */
public class AsyncResponseFilter implements SwaggerSpecFilter {

    @Override
    public boolean isOperationAllowed(Operation operation, ApiDescription api, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        return true;
    }

    @Override
    public boolean isParamAllowed(Parameter parameter, Operation operation, ApiDescription api, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        if (parameter.dataType().equals("AsyncResponse")) { // ignoring AsyncResponse parameters
            return false;
        }
        return true;
    }
}
