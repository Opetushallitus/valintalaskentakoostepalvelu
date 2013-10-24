package fi.vm.sade.valinta.kooste.kela;

import static fi.vm.sade.valinta.kooste.util.TarjontaUriToKoodistoUtil.toSearchCriteria;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.ByteArrayInputStream;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.koodisto.service.types.common.KoodiType;
import fi.vm.sade.tarjonta.service.resources.dto.HakuDTO;
import fi.vm.sade.valinta.kooste.dto.DateParam;
import fi.vm.sade.valinta.kooste.kela.dto.KelaCacheDocument;
import fi.vm.sade.valinta.kooste.kela.dto.KelaHeader;
import fi.vm.sade.valinta.kooste.kela.proxy.KelaExportProxy;
import fi.vm.sade.valinta.kooste.kela.proxy.KelaFtpProxy;
import fi.vm.sade.valinta.kooste.tarjonta.TarjontaHakuProxy;

@Path("kela")
@Controller
public class KelaAktivointiResource {

    private static final Logger LOG = LoggerFactory.getLogger(KelaAktivointiResource.class);

    @Autowired
    private KelaExportProxy kelaExportProxy;

    @Autowired
    private KelaCache kelaCache;
    @Autowired
    private KelaFtpProxy kelaFtpProxy;
    @Autowired
    private TarjontaHakuProxy hakuProxy;
    @Autowired
    private KoodiService koodiService;

    @GET
    @Path("aktivoi")
    public Response aktivoiKelaTiedostonluonti(@QueryParam("hakuOid") String hakuOid,
            @QueryParam("hakukohdeOid") String hakukohdeOid, @QueryParam("lukuvuosi") DateParam l,
            @QueryParam("poimintapaivamaara") DateParam poimintapaivamaara) {
        int lukuvuosi = 2014;
        int kuukausi = 1;
        try {
            HakuDTO hakuDTO = hakuProxy.haeHaku(hakuOid);
            lukuvuosi = hakuDTO.getKoulutuksenAlkamisVuosi();
            // kausi_k
            for (KoodiType koodi : koodiService.searchKoodis(toSearchCriteria(hakuDTO.getKoulutuksenAlkamiskausiUri()))) {
                if ("S".equals(StringUtils.upperCase(koodi.getKoodiArvo()))) { // syksy
                    kuukausi = 8;
                }
                LOG.error("Viallinen arvo {}, koodilla {} ",
                        new Object[] { koodi.getKoodiArvo(), hakuDTO.getKoulutuksenAlkamiskausiUri() });
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Ei voitu hakea lukuvuotta tarjonnalta syystä {}", e.getMessage());
        }
        try {
            kelaCache.addDocument(KelaCacheDocument.createInfoMessage("Dokumentin luonti aloitettu"));
            //
            // HUOM! Muuttuu niin etta kela tiedostoja luodaan useammalle haulle
            // kerralla
            //

            kelaExportProxy.luoTKUVAYHVA(hakuOid, new DateTime(lukuvuosi, kuukausi, 1, 1, 1).toDate(),
                    poimintapaivamaara.getDate(), SecurityContextHolder.getContext().getAuthentication());

            return Response.ok().build();
        } catch (Exception e) {
            LOG.error("Kelatiedoston luonti epäonnistui {}", e.getMessage());
            kelaCache.addDocument(KelaCacheDocument.createErrorMessage("Dokumentin luonti epäonnistui!"));
            return Response.serverError().build();
        }
    }

    @GET
    @Path("lataa/{documentId}")
    public Response lataa(@PathParam("documentId") String input, @Context HttpServletResponse response) {
        KelaCacheDocument document = kelaCache.getDocument(input);
        if (document == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        response.setHeader("Content-Type", "application/TKUVA.YHVA14");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + document.getHeader() + "\"");
        response.setHeader("Content-Length", String.valueOf(document.getData().length));
        return Response.ok(document.getData()).type("application/TKUVA.YHVA14").build();
    }

    @PUT
    @Path("laheta/{documentId}")
    public Response laheta(@PathParam("documentId") String input) {
        KelaCacheDocument document = kelaCache.getDocument(input);
        if (document == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        try {
            kelaFtpProxy.lahetaTiedosto(document.getHeader(), new ByteArrayInputStream(document.getData()));
        } catch (Exception e) {
            kelaCache.addDocument(KelaCacheDocument.createErrorMessage("FTP-lähetys epäonnistui!"));
            return Response.serverError().build();
        }
        kelaCache.addDocument(KelaCacheDocument.createInfoMessage("Dokumentti " + document.getHeader()
                + " lähetetty Kelan FTP-palvelimelle"));
        return Response.ok().build();
    }

    @GET
    @Path("listaus")
    @Produces(APPLICATION_JSON)
    public Collection<KelaHeader> listaus() {
        return kelaCache.getHeaders();
    }
}
