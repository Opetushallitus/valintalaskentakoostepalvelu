package fi.vm.sade.valinta.kooste.pistesyotto.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultOrganization;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.UlkoinenResponseDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.VirheDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.service.HakukohdeOIDAuthorityCheck;
import fi.vm.sade.valinta.kooste.pistesyotto.service.PistesyottoTuontiService;
import fi.vm.sade.valinta.kooste.pistesyotto.service.PistesyottoTuontiSoteliService;
import fi.vm.sade.valinta.kooste.pistesyotto.service.PistesyottoVientiService;
import fi.vm.sade.valinta.kooste.security.AuthorityCheckService;
import fi.vm.sade.valinta.kooste.util.SecurityUtil;
import org.apache.camel.Produce;
import org.apache.poi.util.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
import rx.*;
import rx.Observable;

import static fi.vm.sade.auditlog.valintaperusteet.LogMessage.builder;
import static fi.vm.sade.valinta.kooste.KoosteAudit.AUDIT;
import static fi.vm.sade.valinta.kooste.util.SecurityUtil.*;
import static java.util.Arrays.*;

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
    @Autowired
    private PistesyottoTuontiSoteliService tuontiSoteliService;
    @Autowired
    private AuthorityCheckService authorityCheckService;

    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @POST
    @Path("/vienti")
    @Consumes("application/json")
    @Produces("application/json")
    @ApiOperation(consumes = "application/json", value = "Pistesyötön vienti taulukkolaskentaan", response = ProsessiId.class)
    public ProsessiId vienti(@QueryParam("hakuOid") String hakuOid, @QueryParam("hakukohdeOid") String hakukohdeOid) {
        DokumenttiProsessi prosessi = new DokumenttiProsessi("Pistesyöttö", "vienti", hakuOid, asList(hakukohdeOid));
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
        final String username = KoosteAudit.username();
        ByteArrayOutputStream xlsx;
        IOUtils.copy(file, xlsx = new ByteArrayOutputStream());
        IOUtils.closeQuietly(file);
        try {
            final String uuid = UUID.randomUUID().toString();
            Long expirationTime = DateTime.now().plusDays(7).toDate().getTime();
            List<String> tags = asList();
            dokumenttiAsyncResource.tallenna(uuid, "pistesyotto.xlsx", expirationTime, tags,
                    "application/octet-stream", new ByteArrayInputStream(xlsx.toByteArray()), response -> {
                        LOG.info("Käyttäjä {} aloitti pistesyötön tuonnin haussa {} ja hakukohteelle {}. Excel on tallennettu dokumenttipalveluun uuid:lla {} 7 päiväksi.", username, hakuOid, hakukohdeOid, uuid);
                    }, poikkeus -> {
                        LOG.error("Käyttäjä {} aloitti pistesyötön tuonnin haussa {} ja hakukohteelle {}. Exceliä ei voitu tallentaa dokumenttipalveluun.",
                                username, hakuOid, hakukohdeOid);
                        LOG.error("Virheen tiedot", poikkeus);
                    });
        } catch (Throwable t) {
            LOG.error("Tuntematon virhetilanne", t);
        }
        DokumenttiProsessi prosessi = new DokumenttiProsessi("Pistesyöttö", "tuonti", hakuOid, asList(hakukohdeOid));
        dokumenttiKomponentti.tuoUusiProsessi(prosessi);
        tuontiService.tuo(username, hakuOid, hakukohdeOid, prosessi, new ByteArrayInputStream(xlsx.toByteArray()));
        return prosessi.toProsessiId();
    }

    @POST
    @Path("/ulkoinen")
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @Consumes("application/json")
    @Produces("application/json")
    @ApiOperation(consumes = "application/json", value = "Pistesyötön tuonti hakemuksille ulkoisesta järjestelmästä", response = UlkoinenResponseDTO.class)
    public void ulkoinenTuonti(@QueryParam("hakuOid") String hakuOid,
                                              @QueryParam("valinnanvaiheOid") String valinnanvaiheOid,
                                              List<HakemusDTO> hakemukset,
                                              @Suspended AsyncResponse asyncResponse) {




        if(hakemukset == null || hakemukset.isEmpty()) {
            asyncResponse.resume(Response.serverError().entity("Ulkoinen pistesyotto API requires at least one hakemus").build());
        } else {
            asyncResponse.setTimeout(30L, TimeUnit.MINUTES);
            asyncResponse.setTimeoutHandler(asyncResponse1 -> {
                LOG.error("Ulkoinen pistesyotto -palvelukutsu on aikakatkaistu: /haku/{}", hakuOid);
                asyncResponse1.resume(Response.serverError().entity("Ulkoinen pistesyotto -palvelukutsu on aikakatkaistu").build());
            });

            final String username = KoosteAudit.username();
            authorityCheckService.getAuthorityCheckForRoles(asList("ROLE_APP_HAKEMUS_READ_UPDATE", "ROLE_APP_HAKEMUS_CRUD", "ROLE_APP_HAKEMUS_LISATIETORU", "ROLE_APP_HAKEMUS_LISATIETOCRUD"),
                    authorityCheck -> {
                        LOG.info("Pisteiden tuonti ulkoisesta järjestelmästä (haku: {}): {}", hakuOid, hakemukset);
                        tuontiSoteliService.tuo(authorityCheck, hakemukset,username, hakuOid, valinnanvaiheOid,
                                (onnistuneet, validointivirheet) -> {
                                    UlkoinenResponseDTO response = new UlkoinenResponseDTO();
                                    response.setKasiteltyOk(onnistuneet);
                                    response.setVirheet(Lists.newArrayList(validointivirheet));
                                    asyncResponse.resume(Response.ok(response).build());
                                },
                                sisainenPoikkeus -> {
                                    LOG.error("Soteli tuonti epaonnistui!", sisainenPoikkeus);
                                    asyncResponse.resume(Response.serverError().entity(sisainenPoikkeus.toString()).build());
                                });
                    },
                    sisainenPoikkeus -> {
                        LOG.error("Soteli tuonti epaonnistui!", sisainenPoikkeus);
                        asyncResponse.resume(Response.serverError().entity(sisainenPoikkeus.toString()).build());

                    });




        }
    }


}
