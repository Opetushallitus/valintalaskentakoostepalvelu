package fi.vm.sade.valinta.kooste.test;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jussi Jartamo
 */
@Path("/smoketest")
public class SmoketestResource {

    private static final Logger LOG = LoggerFactory.getLogger(SmoketestResource.class);
    public static final AtomicInteger SECURED_SERVICE_CALL_COUNTER = new AtomicInteger(0);
    public static final AtomicInteger UNSECURED_SERVICE_CALL_COUNTER = new AtomicInteger(0);

    @GET
    @Path("/secured_service_call")
    @PreAuthorize("isAuthenticated()")
    public Response securedOperation() {
        LOG.info("Secured service call activated");
        return Response.ok(SECURED_SERVICE_CALL_COUNTER.incrementAndGet()).build();
    }
    @GET
    @Path("/unsecured_service_call")
    public Response unsecureOperation() {
        LOG.info("Unsecured service call activated");
        return Response.ok(UNSECURED_SERVICE_CALL_COUNTER.incrementAndGet()).build();
    }
}
