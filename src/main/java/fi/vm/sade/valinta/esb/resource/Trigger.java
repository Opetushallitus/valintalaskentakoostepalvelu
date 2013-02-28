package fi.vm.sade.valinta.esb.resource;

import java.lang.System;


import javax.ws.rs.GET;
import javax.ws.rs.Path;

import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;


/**
 *
 * @author Kari Kammonen
 */
@Path("/")
public class Trigger {

    @GET
    @Path("/laske")
    public String valintalaskenta(@QueryParam("hakukohdeOid") String hakukohdeOid,
                                              @QueryParam("valinnanvaihe") String valinnanvaihe) {

        return null;
    }



}
