package fi.vm.sade.valinta.kooste.hakutoiveet;

import java.util.Date;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Jersey REST rajapinta Camel-reitityksen aktivointiin. Ei
 *         varsinaisesti liity Cameliin mutta käyttää Camelin
 *         Proxyä(HakutoiveetAktivointiProxy) reitin käynnistykseen.
 */
@Controller
@Path("/hakutoiveet")
public class HakutoiveetAktivointiResource {

    private static final Logger LOG = LoggerFactory.getLogger(HakutoiveetAktivointiResource.class);

    @Autowired
    private HakutoiveetAktivointiProxy hakutoiveetProxy;

    /**
     * "http://stackoverflow.com/questions/974079/setting-mime-type-for-excel-document"
     * 
     * @param hakutoiveetOid
     * @return
     */
    @GET
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Path("/aktivoi")
    public Response aktivoiHakutoiveidenHaku(@QueryParam("hakutoiveOid") String hakutoiveetOid) {
        LOG.info("Hakutoiveiden({}) haku aktivoitu REST-resurssista!", hakutoiveetOid);
        return Response.status(200).entity(hakutoiveetProxy.aktivoiHakutoiveetReitti(hakutoiveetOid))
                .header("Content-disposition", "attachment;filename=" + hakutoiveetOid + new Date() + ".xslx").build();
    }

}
