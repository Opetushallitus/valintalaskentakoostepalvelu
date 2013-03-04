package fi.vm.sade.valinta.kooste.hakutoiveet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

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

    @GET
    @Path("/aktivoi")
    public void aktivoiHakutoiveidenHaku(@QueryParam("hakutoiveOid") String hakutoiveetOid) {
        hakutoiveetProxy.aktivoiHakutoiveetReitti(hakutoiveetOid);
        LOG.info("Hakutoiveiden({}) haku aktivoitu REST-resurssista!", hakutoiveetOid);
    }

}
