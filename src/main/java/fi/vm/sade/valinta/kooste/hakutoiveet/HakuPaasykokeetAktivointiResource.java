package fi.vm.sade.valinta.kooste.hakutoiveet;

import java.text.SimpleDateFormat;
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
public class HakuPaasykokeetAktivointiResource {

    private static final Logger LOG = LoggerFactory.getLogger(HakuPaasykokeetAktivointiResource.class);

    private static final SimpleDateFormat formatter = new SimpleDateFormat("dd_MM_yyyy");

    @Autowired
    private HakuPaasykokeetAktivointiProxy hakutoiveetProxy;

    /**
     * "http://stackoverflow.com/questions/974079/setting-mime-type-for-excel-document"
     * 
     * @param hakutoiveetOid
     * @return Ketkä menee tekemään hakukoetta kyseessä olevaan hakukohteeseen
     */
    @GET
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Path("/aktivoi")
    public Response aktivoiPaasykokeidenHaku(@QueryParam("hakuOid") String hakuOid) {
        LOG.info("Hakuoid({}) haku aktivoitu REST-resurssista!", hakuOid);
        return Response
                .status(200)
                .entity(hakutoiveetProxy.aktivoiHakuPaasykokeetReitti(hakuOid))
                .header("Content-disposition",
                        "attachment;filename=" + hakuOid + "-" + formatter.format(new Date()) + ".xslx").build();
    }

}
