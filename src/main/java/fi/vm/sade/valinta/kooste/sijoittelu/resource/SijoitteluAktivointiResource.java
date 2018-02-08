package fi.vm.sade.valinta.kooste.sijoittelu.resource;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.parametrit.service.HakuParametritService;
import fi.vm.sade.valinta.kooste.tarjonta.api.OrganisaatioResource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.google.gson.Gson;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.sijoittelu.dto.DelayedSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.Sijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.JatkuvaSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.route.SijoitteluAktivointiRoute;
import fi.vm.sade.valinta.kooste.sijoittelu.route.SijoittelunValvonta;
import fi.vm.sade.valinta.seuranta.resource.SijoittelunSeurantaResource;
import fi.vm.sade.valinta.seuranta.sijoittelu.dto.SijoitteluDto;

import static fi.vm.sade.auditlog.valintaperusteet.LogMessage.builder;
import static fi.vm.sade.valinta.kooste.KoosteAudit.AUDIT;

/**
 * @Autowired(required = false) Camel-reitit valinnaisiksi poisrefaktorointia odotellessa.
 */
@Controller("SijoitteluAktivointiResource")
@Path("koostesijoittelu")
@PreAuthorize("isAuthenticated()")
@Api(value = "/koostesijoittelu", description = "Ohjausparametrit palveluiden aktiviteettipäivämäärille")
public class SijoitteluAktivointiResource {
    private static final Logger LOG = LoggerFactory.getLogger(SijoitteluAktivointiResource.class);
    public static final String OPH_CRUD = "hasAnyRole('ROLE_APP_SIJOITTELU_CRUD_1.2.246.562.10.00000000001')";
    public static final String ANY_CRUD = "hasAnyRole('ROLE_APP_SIJOITTELU_CRUD')";

    @Autowired(required = false)
    private SijoitteluAktivointiRoute sijoitteluAktivointiProxy;

    @Autowired(required = false)
    private JatkuvaSijoittelu jatkuvaSijoittelu;

    @Autowired
    private HakuParametritService hakuParametritService;

    @Autowired
    private SijoittelunSeurantaResource sijoittelunSeurantaResource;

    @Autowired
    private SijoittelunValvonta sijoittelunValvonta;

    @Autowired
    private TarjontaAsyncResource tarjontaResource;

    @Autowired
    private OrganisaatioResource organisaatioProxy;

    @GET
    @Path("/status/{hakuoid}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Sijoittelun status", response = String.class)
    public Sijoittelu status(@PathParam("hakuoid") String hakuOid) {
        return sijoittelunValvonta.haeAktiivinenSijoitteluHaulle(hakuOid);
    }

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Jatkuvan sijoittelun jonossa olevat sijoittelut", response = String.class)
    public Collection<DelayedSijoittelu> status() {
        return jatkuvaSijoittelu.haeJonossaOlevatSijoittelut();
    }

    @POST
    @Path("/aktivoi")
    @PreAuthorize("hasAnyRole('ROLE_APP_VALINTOJENTOTEUTTAMINEN_CRUD')")
    @ApiOperation(value = "Sijoittelun aktivointi", response = String.class)
    public void aktivoiSijoittelu(@QueryParam("hakuOid") String hakuOid) {
        if (!hakuParametritService.getParametritForHaku(hakuOid).valinnanhallintaEnabled()) {
            LOG.error("Sijoittelua yritettiin käynnistää haulle({}) ilman käyttöoikeuksia!", hakuOid);
            throw new RuntimeException("Ei käyttöoikeuksia!");
        }

        if (StringUtils.isBlank(hakuOid)) {
            LOG.error("Sijoittelua yritettiin käynnistää ilman hakuOidia!");
            throw new RuntimeException("Parametri hakuOid on pakollinen!");
        } else {
            
            final String username = KoosteAudit.username();
            AUDIT.log(builder()
                    .id(username)
                    .hakuOid(hakuOid)
                    .setOperaatio(ValintaperusteetOperation.SIJOITTELU_KAYNNISTYS)
                    .build());

            sijoitteluAktivointiProxy
                    .aktivoiSijoittelu(new Sijoittelu(hakuOid));
        }
    }

    @GET
    @Path("/jatkuva/aktivoi")
    @Produces(MediaType.TEXT_PLAIN)
    @PreAuthorize(OPH_CRUD)
    @ApiOperation(value = "Ajastetun sijoittelun aktivointi", response = String.class)
    public String aktivoiJatkuvassaSijoittelussa(
            @QueryParam("hakuOid") String hakuOid) {
        if (!hakuParametritService.getParametritForHaku(hakuOid).valinnanhallintaEnabled()) {
            return "no privileges.";
        }

        if (StringUtils.isBlank(hakuOid)) {
            return "get parameter 'hakuOid' required";
        } else {
            SijoitteluDto sijoitteluDto = sijoittelunSeurantaResource.hae(hakuOid);
            if(sijoitteluDto.getAloitusajankohta() == null || sijoitteluDto.getAjotiheys() == null) {
                LOG.warn("Haulta {} puuttuu jatkuvan sijoittelun parametreja. Ei aktivoida jatkuvaa sijoittelua.");
                return "ei aktivoitu";
            } else {
                LOG.info("jatkuva sijoittelu aktivoitu haulle {}", hakuOid);
                sijoittelunSeurantaResource.merkkaaSijoittelunAjossaTila(hakuOid, true);
                return "aktivoitu";
            }

        }
    }

    @GET
    @Path("/jatkuva/poista")
    @Produces(MediaType.TEXT_PLAIN)
    @PreAuthorize(OPH_CRUD)
    @ApiOperation(value = "Ajastetun sijoittelun deaktivointi", response = String.class)
    public String poistaJatkuvastaSijoittelusta(
            @QueryParam("hakuOid") String hakuOid) {
        if (!hakuParametritService.getParametritForHaku(hakuOid).valinnanhallintaEnabled()) {
            return "no privileges.";
        }

        if (StringUtils.isBlank(hakuOid)) {
            return "get parameter 'hakuOid' required";
        } else {
            LOG.info("jatkuva sijoittelu poistettu haulta {}", hakuOid);
            sijoittelunSeurantaResource.poistaSijoittelu(hakuOid);
            return "poistettu";
        }
    }

    @GET
    @Path("/jatkuva/kaikki")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize(OPH_CRUD)
    @ApiOperation(value = "Kaikki aktiiviset sijoittelut", response = Map.class)
    public Collection<SijoitteluDto> aktiivisetSijoittelut() {
        return sijoittelunSeurantaResource.hae();
    }

    @GET
    @Path("/jatkuva")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize(ANY_CRUD)
    @ApiOperation(value = "Haun aktiiviset sijoittelut", response = SijoitteluDto.class)
    public void jatkuvaTila(@QueryParam("hakuOid") String hakuOid,
                              @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(2L, TimeUnit.MINUTES);
        asyncResponse.setTimeoutHandler(asyncResponse1 -> {
            LOG.error("Haun aktiiviset sijoittelut -palvelukutsu on aikakatkaistu: /koostesijoittelu/jatkuva/{}", hakuOid);
            asyncResponse1.resume(Response.serverError().entity("Haun aktiiviset sijoittelut -palvelukutsu on aikakatkaistu").build());
        });

        SecurityContext context = SecurityContextHolder.getContext();
        if (context == null) {
            String msg = "No SecurityContext found";
            asyncResponse.resume(new ForbiddenException(msg));
        }

        Authentication authentication = context.getAuthentication();
        if (authentication == null) {
            String msg = "No Authentication found in SecurityContext";
            asyncResponse.resume(new ForbiddenException(msg));
        }

        Collection<? extends GrantedAuthority> userRoles = authentication.getAuthorities();

        tarjontaResource.haeHaku(hakuOid).subscribe(haku -> {
            String organisaatioOid = haku.getTarjoajaOids()[0];

            boolean authorizedForAnyParentOid = false;

            try {
                String parentOidsPath = organisaatioProxy.parentoids(organisaatioOid);
                String[] parentOids = parentOidsPath.split("/");

                for (String oid : parentOids) {
                    String organizationRole = "ROLE_APP_SIJOITTELU_CRUD_" + oid;

                    for (GrantedAuthority auth : userRoles) {
                        if (organizationRole.equals(auth.getAuthority()))
                            authorizedForAnyParentOid = true;
                    }
                }
            } catch (Exception e) {
                String msg = String.format("Organisaation %s parentOids -haku epäonnistui", organisaatioOid);
                LOG.error(msg, e);
                asyncResponse.resume(new ForbiddenException(msg));
            }

            if (authorizedForAnyParentOid) {
                String resp = jatkuvaTilaAutorisoituOrganisaatiolle(hakuOid);
                asyncResponse.resume(resp);
            } else {
                String msg = String.format(
                        "Käyttäjällä ei oikeutta haun %s tarjoajaan %s tai sen yläorganisaatioihin.",
                        hakuOid,
                        organisaatioOid
                );
                LOG.error(msg);
                asyncResponse.resume(new ForbiddenException(msg));
            }
        });
    }

    private String jatkuvaTilaAutorisoituOrganisaatiolle(String hakuOid) {
        if (StringUtils.isBlank(hakuOid)) {
            return null;
        } else {
            SijoitteluDto sijoitteluDto = sijoittelunSeurantaResource.hae(hakuOid);
            return new Gson().toJson(sijoitteluDto);
        }
    }

    @GET
    @Path("/jatkuva/paivita")
    @Produces(MediaType.TEXT_PLAIN)
    @PreAuthorize(OPH_CRUD)
    @ApiOperation(value = "Ajastetun sijoittelun aloituksen päivitys", response = String.class)
    public String paivitaJatkuvanSijoittelunAloitus(
            @QueryParam("hakuOid") String hakuOid,
            @QueryParam("aloitusajankohta") Long aloitusajankohta,
            @QueryParam("ajotiheys") Integer ajotiheys) {
        if (!hakuParametritService.getParametritForHaku(hakuOid).valinnanhallintaEnabled()) {
            return "no privileges.";
        }
        if (StringUtils.isBlank(hakuOid)) {
            return "get parameter 'hakuOid' required";
        } else {
            sijoittelunSeurantaResource.paivitaSijoittelunAloitusajankohta(hakuOid, aloitusajankohta, ajotiheys);
            return "paivitetty";
        }
    }
}
