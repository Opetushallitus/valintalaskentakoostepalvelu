package fi.vm.sade.valinta.kooste.kela;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAALKU;
import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVALOPPU;
import fi.vm.sade.rajapinnat.kela.tkuva.util.KelaUtil;
import fi.vm.sade.valinta.kooste.dto.DateParam;
import fi.vm.sade.valinta.kooste.kela.proxy.TKUVAYHVAExportProxy;

@Path("kela")
@Controller
public class KelaAktivointiResource {

    public final static MediaType APPLICATION_TKUVAYHVA = new MediaType("application", "TKUVA.YHVA14");

    @Autowired
    private TKUVAYHVAExportProxy kelaExportProxy;

    @GET
    @Path("TKUVAYHVA/aktivoi")
    public Response aktivoiKelaTiedostonluonti(@QueryParam("hakuOid") String hakuOid,
            @QueryParam("hakukohdeOid") String hakukohdeOid, @QueryParam("lukuvuosi") DateParam lukuvuosi,
            @QueryParam("poimintapaivamaara") DateParam poimintapaivamaara) {
        try {

            Deque<InputStream> streams = new ArrayDeque<InputStream>(kelaExportProxy.luoTKUVAYHVA(hakuOid,
                    lukuvuosi.getDate(), poimintapaivamaara.getDate()));
            Integer count = streams.size();
            streams.addFirst(new ByteArrayInputStream(new TKUVAALKU.Builder().setAjopaivamaara(new Date())
                    .setAineistonnimi(StringUtils.EMPTY).setOrganisaationimi(StringUtils.EMPTY).build().toByteArray()));
            streams.addLast(new ByteArrayInputStream(new TKUVALOPPU.Builder().setAjopaivamaara(new Date())
                    .setTietuelukumaara(count).build().toByteArray()));

            return Response.ok(new SequenceInputStream(Collections.enumeration(streams)), APPLICATION_TKUVAYHVA)
                    .header("content-disposition", "inline; filename=" + KelaUtil.createTiedostoNimiYhva14(new Date()))
                    .build();
        } catch (Exception e) {
            return Response.noContent().build();
            // ok(input, APPLICATION_TKUVAYHVA).header("content-disposition",
            // "inline; filename=").build();
        }
    }
}
