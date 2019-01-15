package fi.vm.sade.valinta.kooste.valintakokeet;

import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import io.reactivex.Observable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

@Controller
@PreAuthorize("isAuthenticated()")
@Path("/valintakoe")
@Api(value = "/valintakoe", description = "Resurssi valintakoeosallistumistulosten hakemiseen.")
public class AktiivistenHakemustenValintakoeResource {
    private static final String VALINTAKAYTTAJA_ROLE = "hasAnyRole('ROLE_APP_VALINTOJENTOTEUTTAMINEN_READ'," +
            "'ROLE_APP_VALINTOJENTOTEUTTAMINEN_READ_UPDATE','ROLE_APP_VALINTOJENTOTEUTTAMINEN_CRUD')";
    private final static Logger LOG = LoggerFactory.getLogger(AktiivistenHakemustenValintakoeResource.class);

    private final ValintalaskentaValintakoeAsyncResource valintakoeAsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private AtaruAsyncResource ataruAsyncResource;
    private final TarjontaAsyncResource tarjontaAsyncResource;

    @Autowired
    public AktiivistenHakemustenValintakoeResource(ValintalaskentaValintakoeAsyncResource valintakoeAsyncResource,
                                                   ApplicationAsyncResource applicationAsyncResource,
                                                   AtaruAsyncResource ataruAsyncResource,
                                                   TarjontaAsyncResource tarjontaAsyncResource) {
        this.valintakoeAsyncResource = valintakoeAsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.ataruAsyncResource = ataruAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("hakutoive/{hakukohdeOid}")
    @PreAuthorize(VALINTAKAYTTAJA_ROLE)
    @ApiOperation(value = "Hakee valintakoeosallistumiset hakukohteelle OID:n perusteella, " +
            "filtteröiden pois passiiviset hakemukset", response = ValintakoeOsallistuminenDTO.class)
    public void osallistumisetByHakutoive(
            @ApiParam(value = "Hakukohde OID", required = true) @PathParam("hakukohdeOid") String hakukohdeOid,
            @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(30, TimeUnit.SECONDS);

        valintakoeAsyncResource.haeHakutoiveelle(hakukohdeOid)
                .flatMap(osallistumiset ->
                        filtteroiPoisPassiivistenHakemustenOsallistumistiedot(osallistumiset, hakukohdeOid))
                .subscribe(osallistumiset ->
                                asyncResponse.resume(Response.ok(osallistumiset, APPLICATION_JSON_TYPE).build()),
                        exception -> {
                            String message = String.format("Virhe haettaessa valintakoeosallistumisia hakukohteelle %s",
                                    hakukohdeOid);
                            LOG.error(message, exception);
                            asyncResponse.resume(Response
                                    .serverError().entity(String.format("%s : %s", message, exception.getMessage())).build());
                        });
    }

    private Observable<List<ValintakoeOsallistuminenDTO>> filtteroiPoisPassiivistenHakemustenOsallistumistiedot(
            List<ValintakoeOsallistuminenDTO> osallistumiset, String hakukohdeOid) {
        List<String> kaikkiOsallistumistenHakemusOidit = osallistumiset.stream()
                .map(ValintakoeOsallistuminenDTO::getHakemusOid).distinct().collect(Collectors.toList());

        return tarjontaAsyncResource.haeHakukohde(hakukohdeOid)
                .flatMap(hakukohde -> tarjontaAsyncResource.haeHaku(hakukohde.getHakuOid()))
                .flatMap(haku -> {
                    if (StringUtils.isEmpty(haku.getAtaruLomakeAvain())) {
                        return applicationAsyncResource.getApplicationsByHakemusOids(kaikkiOsallistumistenHakemusOidit)
                                .map(hakemukset -> hakemukset.stream()
                                        .map(HakemusWrapper::getOid).collect(Collectors.toSet()));
                    } else {
                        return ataruAsyncResource.getApplicationsByOids(kaikkiOsallistumistenHakemusOidit)
                                .map(hakemukset -> hakemukset.stream()
                                        .map(HakemusWrapper::getOid).collect(Collectors.toSet()));
                    }
                })
                .map(aktiivistenHakemusOidit ->
                        osallistumiset.stream().filter(o -> {
                            boolean onAktiivinen = aktiivistenHakemusOidit.contains(o.getHakemusOid());
                            if (!onAktiivinen) {
                                LOG.warn(String.format("Hakemuksen %s valintakoeosallistuminen filtteröidään pois " +
                                                "haettaessa hakukohteen %s osallistumistietoja, " +
                                                "koska hakemusnumerolla ei löydy aktiivista hakemusta.",
                                        o.getHakemusOid(), hakukohdeOid));
                            }
                            return onAktiivinen;
                        }).collect(Collectors.toList()));
    }
}
