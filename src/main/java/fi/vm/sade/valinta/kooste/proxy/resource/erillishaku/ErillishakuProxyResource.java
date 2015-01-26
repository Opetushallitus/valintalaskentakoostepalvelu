package fi.vm.sade.valinta.kooste.proxy.resource.erillishaku;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.dto.HakemusSijoitteluntulosMergeDto;
import static fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.util.HakemusSijoitteluntulosMergeUtil.*;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Jussi Jartamo
 */
@Controller
@Path("proxy/erillishaku")
@PreAuthorize("isAuthenticated()")
@Api(value = "/proxy/erillishaku", description = "Käyttöliittymäkutsujen välityspalvelin haku-app:n ja valinta-tulos-serviceen")
public class ErillishakuProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(ErillishakuProxyResource.class);

    //@Autowired
    //private ValintaTulosServiceAsyncResource valintaTulosService;
    @Autowired
    private ApplicationAsyncResource applicationAsyncResource;
    @Autowired
    private SijoitteluAsyncResource sijoitteluAsyncResource;

    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @GET
    @Path("/haku/{hakuOid}/hakukohde/{hakukohdeOid}")
    @Consumes("application/json")
    @ApiOperation(consumes = "application/json", value = "Hakukohteen valintatulokset", response = ProsessiId.class)
    public void vienti(
            @PathParam("hakuOid") String hakuOid,
            @PathParam("hakukohdeOid") String hakukohdeOid,
            @Suspended AsyncResponse asyncResponse) {
        //authorizer.checkOrganisationAccess(tarjoajaOid, ROLE_APP_HAKEMUS_CRUD);
        asyncResponse.setTimeout(1L, TimeUnit.MINUTES);
        asyncResponse.setTimeoutHandler(new TimeoutHandler() {
            public void handleTimeout(AsyncResponse asyncResponse) {
                LOG.error(
                        "Erillishakuproxy -palvelukutsu on aikakatkaistu: /haku/{}/hakukohde/{}",
                        hakuOid, hakukohdeOid);
                asyncResponse.resume(Response.serverError()
                        .entity("Erillishakuproxy -palvelukutsu on aikakatkaistu")
                        .build());
            }
        });

        final AtomicReference<Object> tmp = new AtomicReference<>();
        // kertaluokassa nopeampaa kuin futureilla, koska säikeet ei blokkaile
        applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohdeOid,
                hakemukset -> {
                    if(!tmp.compareAndSet(null,hakemukset)) {
                        r(asyncResponse, merge(hakemukset, (HakukohdeDTO) tmp.get()));
                    }
                },
                poikkeus -> {
                    try {
                        asyncResponse.resume(Response.serverError()
                                .entity("Erillishakuproxy -palvelukutsu epäonnistui haku-app:n virheeseen: " + poikkeus.getMessage())
                                .build());
                    } catch (Exception e) {
                        // kilpailutilanne timeoutin ja virheen kanssa. Oikeastaan sama mitä käyttäjälle näytetään tässä kohtaa.
                        LOG.error("Haku-app virhe tuli yhtäaikaa timeoutin kanssa! {}", e.getMessage());
                    }
                }
        );

        sijoitteluAsyncResource.getLatestHakukohdeBySijoittelu(hakuOid, hakukohdeOid,
                hakukohde -> {
                    if(!tmp.compareAndSet(null,hakukohde)) {
                        r(asyncResponse, merge((List<Hakemus>) tmp.get(),hakukohde));
                    }
                },
                poikkeus -> {
                    try {
                        asyncResponse.resume(Response.serverError()
                                .entity("Erillishakuproxy -palvelukutsu epäonnistui valintatulosservicen virheeseen: " + poikkeus.getMessage())
                                .build());
                    } catch (Exception e) {
                        // kilpailutilanne timeoutin ja virheen kanssa. Oikeastaan sama mitä käyttäjälle näytetään tässä kohtaa.
                        LOG.error("Valintatulosservice virhe tuli yhtäaikaa timeoutin kanssa! {}", e.getMessage());
                    }
                }
        );
    }
    private void r(AsyncResponse asyncResponse, List<HakemusSijoitteluntulosMergeDto> msg) {
        asyncResponse.resume(Response.ok(msg).build());
    }


}
