package fi.vm.sade.valinta.kooste.pistesyotto.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.pistesyotto.service.PistesyottoTuontiService;
import fi.vm.sade.valinta.kooste.pistesyotto.service.PistesyottoVientiService;
import org.apache.camel.Produce;
import org.apache.poi.util.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;

@Controller("PistesyottoResource")
@Path("pistesyotto")
@PreAuthorize("isAuthenticated()")
@Api(value = "/pistesyotto", description = "Pistesyötön tuonti ja vienti taulukkolaskentaan")
public class PistesyottoResource {
    private static final Logger LOG = LoggerFactory.getLogger(PistesyottoResource.class);

    @Autowired
    private DokumenttiProsessiKomponentti dokumenttiKomponentti;
    @Autowired
    private DokumenttiAsyncResource dokumenttiAsyncResource;
    @Autowired
    private PistesyottoVientiService vientiService;
    @Autowired
    private PistesyottoTuontiService tuontiService;

    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @POST
    @Path("/vienti")
    @Consumes("application/json")
    @Produces("application/json")
    @ApiOperation(consumes = "application/json", value = "Pistesyötön vienti taulukkolaskentaan", response = ProsessiId.class)
    public ProsessiId vienti(@QueryParam("hakuOid") String hakuOid, @QueryParam("hakukohdeOid") String hakukohdeOid) {
        DokumenttiProsessi prosessi = new DokumenttiProsessi("Pistesyöttö", "vienti", hakuOid, Arrays.asList(hakukohdeOid));
        dokumenttiKomponentti.tuoUusiProsessi(prosessi);
        vientiService.vie(hakuOid, hakukohdeOid, prosessi);
        return prosessi.toProsessiId();
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @POST
    @Path("/tuonti")
    @Consumes("application/octet-stream")
    @Produces("application/json")
    @ApiOperation(consumes = "application/json", value = "Pistesyötön tuonti taulukkolaskentaan", response = ProsessiId.class)
    public ProsessiId tuonti(@QueryParam("hakuOid") String hakuOid, @QueryParam("hakukohdeOid") String hakukohdeOid, InputStream file) throws IOException {
        ByteArrayOutputStream xlsx;
        IOUtils.copy(file, xlsx = new ByteArrayOutputStream());
        IOUtils.closeQuietly(file);
        try {
            final String uuid = UUID.randomUUID().toString();
            Long expirationTime = DateTime.now().plusDays(7).toDate().getTime();
            List<String> tags = Arrays.asList();
            dokumenttiAsyncResource.tallenna(uuid, "pistesyotto.xlsx", expirationTime, tags,
                    "application/octet-stream", new ByteArrayInputStream(xlsx.toByteArray()), response -> {
                        LOG.error("Käyttäjä {} aloitti pistesyötön tuonnin haussa {} ja hakukohteelle {}. Excel on tallennettu dokumenttipalveluun uuid:lla {} 7 päiväksi.", SecurityContextHolder.getContext().getAuthentication().getName(), hakuOid, hakukohdeOid, uuid);
                    }, poikkeus -> {
                        LOG.error("Käyttäjä {} aloitti pistesyötön tuonnin haussa {} ja hakukohteelle {}. Exceliä ei voitu tallentaa dokumenttipalveluun.", SecurityContextHolder.getContext().getAuthentication().getName(), hakuOid, hakukohdeOid, poikkeus);
                    });
        } catch (Throwable t) {
        }
        DokumenttiProsessi prosessi = new DokumenttiProsessi("Pistesyöttö", "tuonti", hakuOid, Arrays.asList(hakukohdeOid));
        dokumenttiKomponentti.tuoUusiProsessi(prosessi);
        tuontiService.tuo(hakuOid, hakukohdeOid, prosessi, new ByteArrayInputStream(xlsx.toByteArray()));
        return prosessi.toProsessiId();
    }
}
