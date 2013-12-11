package fi.vm.sade.valinta.kooste.viestintapalvelu.resource;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

@Path("api/v1")
public interface ViestintapalveluResource {

    @POST
    @Produces(TEXT_PLAIN)
    @Consumes(APPLICATION_JSON)
    @Path("addresslabel/pdf")
    Response haeOsoitetarrat(Osoitteet osoitteet);

    @POST
    @Produces(TEXT_PLAIN)
    @Consumes(APPLICATION_JSON)
    @Path("jalkiohjauskirje/asynczip")
    Response haeJalkiohjauskirjeet(Kirjeet kirjeet);

    @POST
    @Produces(TEXT_PLAIN)
    @Consumes(APPLICATION_JSON)
    @Path("hyvaksymiskirje/pdf")
    Response haeHyvaksymiskirjeet(Kirjeet kirjeet);

    @POST
    @Consumes(TEXT_PLAIN)
    @Path("message")
    Response message(String message);

}
