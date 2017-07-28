package fi.vm.sade.valinta.kooste.util;

import com.google.common.collect.ImmutableMap;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ResponseUtil {
    public static void respondWithError(AsyncResponse asyncResponse, String error) {
        respondWithError(asyncResponse, error, Response.Status.INTERNAL_SERVER_ERROR);
    }

    public static void respondWithError(AsyncResponse asyncResponse, String error, Response.Status status) {
        asyncResponse.resume(Response.status(status)
                .entity(ImmutableMap.of("error", error))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());
    }
}
