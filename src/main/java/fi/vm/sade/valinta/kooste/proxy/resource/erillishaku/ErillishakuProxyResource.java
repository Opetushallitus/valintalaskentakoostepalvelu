package fi.vm.sade.valinta.kooste.proxy.resource.erillishaku;

import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.service.valintaperusteet.dto.model.ValinnanVaiheTyyppi;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;

import static fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.util.HakemusSijoitteluntulosMergeUtil.*;

import fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.dto.MergeValinnanvaiheDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;

import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValintatapajonoDTO;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

///valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/{hakukohdeOid}/valinnanvaihe
///haku-app/applications/listfull?appStates=ACTIVE&appStates=INCOMPLETE&rows=100000&aoOid={hakukohdeOid}&asId={hakuOid}
///sijoittelu-service/resources/sijoittelu/{hakuOid}/sijoitteluajo/latest/hakukohde/{hakukohdeOid}
///valintalaskenta-laskenta-service/resources/valintalaskentakoostepalvelu/hakukohde/{hakukohdeOid}/valinnanvaihe
///sijoittelu-service/resources/tila/hakukohde/{hakukohdeOid}
@Controller("ErillishakuProxyResource")
@Path("proxy/erillishaku")
@PreAuthorize("isAuthenticated()")
@Api(value = "/proxy/erillishaku", description = "Käyttöliittymäkutsujen välityspalvelin haku-app:n ja valinta-tulos-serviceen")
public class ErillishakuProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(ErillishakuProxyResource.class);

    @Autowired
    private TilaAsyncResource tilaResource;
    @Autowired
    private ApplicationAsyncResource applicationAsyncResource;
    @Autowired
    private SijoitteluAsyncResource sijoitteluAsyncResource;
    @Autowired
    private ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    @Autowired
    private ValintalaskentaAsyncResource valintalaskentaAsyncResource;

    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @GET
    @Path("/haku/{hakuOid}/hakukohde/{hakukohdeOid}")
    @Consumes("application/json")
    @ApiOperation(consumes = "application/json", value = "Hakukohteen valintatulokset", response = ProsessiId.class)
    public void vienti(
            @PathParam("hakuOid") String hakuOid,
            @PathParam("hakukohdeOid") String hakukohdeOid,
            @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(1L, TimeUnit.MINUTES);
        asyncResponse.setTimeoutHandler(asyncResponse1 -> {
            LOG.error("Erillishakuproxy -palvelukutsu on aikakatkaistu: /haku/{}/hakukohde/{}", hakuOid, hakukohdeOid);
            asyncResponse1.resume(Response.serverError().entity("Erillishakuproxy -palvelukutsu on aikakatkaistu").build());
        });
        final AtomicReference<List<Hakemus>> hakemukset = new AtomicReference<>();
        final AtomicReference<List<ValinnanVaiheJonoillaDTO>> valinnanvaiheet = new AtomicReference<>();
        final AtomicReference<HakukohdeDTO> hakukohde = new AtomicReference<>();
        final AtomicReference<List<Valintatulos>> vtsValintatulokset = new AtomicReference<>();
        final AtomicReference<List<ValintatietoValinnanvaiheDTO>> valintatulokset = new AtomicReference<>();
        final AtomicReference<Map<Long, HakukohdeDTO>> hakukohteetBySijoitteluAjoId = new AtomicReference<>();
        AtomicInteger counter = new AtomicInteger(1 + 1 + 1 + 1 + 2);

        Supplier<Void> mergeSuplier = () -> {
            if (counter.decrementAndGet() == 0) {
                LOG.info("Saatiin vastaus muodostettua hakukohteelle {} haussa {}. Palautetaan se asynkronisena paluuarvona.", hakukohdeOid, hakuOid);
                r(asyncResponse, merge(hakuOid, hakukohdeOid, hakemukset.get(), hakukohde.get(), valinnanvaiheet.get(), valintatulokset.get(), hakukohteetBySijoitteluAjoId.get(), vtsValintatulokset.get()));
            }
            return null;
        };
        ///haku-app/applications/listfull?appStates=ACTIVE&appStates=INCOMPLETE&rows=100000&aoOid={hakukohdeOid}&asId={hakuOid}
        fetchHakemus(hakuOid, hakukohdeOid, asyncResponse, hakemukset, mergeSuplier);
        ///valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/{hakukohdeOid}/valinnanvaihe
        fetchValinnanVaihes(hakukohdeOid, asyncResponse, valinnanvaiheet, mergeSuplier);
        ///sijoittelu-service/resources/sijoittelu/{hakuOid}/sijoitteluajo/latest/hakukohde/{hakukohdeOid}
        fetchSijoittelu(hakuOid, hakukohdeOid, asyncResponse, hakukohde, mergeSuplier);
        ///sijoittelu-service/resources/tila/hakukohde/{hakukohdeOid}
        fetchValintatulos(hakuOid, hakukohdeOid, asyncResponse, vtsValintatulokset, mergeSuplier);
        ///valintalaskenta-laskenta-service/resources/valintalaskentakoostepalvelu/hakukohde/{hakukohdeOid}/valinnanvaihe
        fetchValinnanTulos(hakuOid, hakukohdeOid, asyncResponse, valintatulokset, hakukohteetBySijoitteluAjoId, mergeSuplier);
    }

    void fetchValinnanTulos(@PathParam("hakuOid") String hakuOid, @PathParam("hakukohdeOid") String hakukohdeOid, @Suspended AsyncResponse asyncResponse, AtomicReference<List<ValintatietoValinnanvaiheDTO>> valintatulokset, AtomicReference<Map<Long, HakukohdeDTO>> hakukohteetBySijoitteluAjoId, Supplier<Void> mergeSuplier) {
        valintalaskentaAsyncResource.laskennantulokset(hakukohdeOid).take(1).subscribe(
                valintatietoValinnanvaihes -> {
                    LOG.info("Haetaan valintalaskennasta tulokset");
                    valintatulokset.set(valintatietoValinnanvaihes);
                    fetchSijoitteluAjoIds(hakuOid, hakukohdeOid, asyncResponse, hakukohteetBySijoitteluAjoId, mergeSuplier, valintatietoValinnanvaihes);
                    mergeSuplier.get();
                },
                poikkeus -> logAndReturnError("valintalaskenta", asyncResponse, poikkeus)
        );
    }

    private void fetchSijoitteluAjoIds(@PathParam("hakuOid") String hakuOid, @PathParam("hakukohdeOid") String hakukohdeOid, @Suspended AsyncResponse asyncResponse, AtomicReference<Map<Long, HakukohdeDTO>> hakukohteetBySijoitteluAjoId, Supplier<Void> mergeSuplier, List<ValintatietoValinnanvaiheDTO> valintatietoValinnanvaihes) {
        Set<Long> sijoitteluAjoIdSetti = valintatietoValinnanvaihes.stream().flatMap(v0 -> v0.getValintatapajonot().stream())
                .filter(v0 -> v0.getSijoitteluajoId() != null)
                .map(ValintatietoValintatapajonoDTO::getSijoitteluajoId).collect(Collectors.toSet());
        // erillinen vaihe missä haetaan vielä n-kappaletta hakukohteen tietoja eri sijoitteluajoid:eillä
        if (!sijoitteluAjoIdSetti.isEmpty()) {
            LOG.info("Saatiin sijoitteluajoid:eitä: {} ja haetaan ne erikseen.", Arrays.toString(sijoitteluAjoIdSetti.toArray()));
            final Map<Long, HakukohdeDTO> erillissijoittelutmp = Maps.newConcurrentMap();
            final AtomicInteger erillissijoitteluCounter = new AtomicInteger(sijoitteluAjoIdSetti.size());
            ///sijoittelu-service/resources/erillissijoittelu/{hakuOid}/sijoitteluajo/{sijoitteluAjoId}/hakukohde/{hakukodeOid}
            sijoitteluAjoIdSetti.forEach(id -> {
                sijoitteluAsyncResource.getLatestHakukohdeBySijoitteluAjoId(hakuOid, hakukohdeOid, id,
                        hakukohde -> {
                            LOG.info("Haettiin laskenta sijoitteluajoid:llä {}.", id);
                            erillissijoittelutmp.put(id, hakukohde);
                            if (erillissijoitteluCounter.decrementAndGet() == 0) {
                                hakukohteetBySijoitteluAjoId.set(erillissijoittelutmp);
                                mergeSuplier.get();
                            }
                        },
                        throwable -> logAndReturnError("valintalaskenta", asyncResponse, throwable)
                );
            });
        } else {
            LOG.info("Ei saatu erillisiä sijoitteluajoideitä.");
            hakukohteetBySijoitteluAjoId.set(Collections.emptyMap());
            mergeSuplier.get();
        }
    }

    void fetchValintatulos(@PathParam("hakuOid") String hakuOid, @PathParam("hakukohdeOid") String hakukohdeOid, @Suspended AsyncResponse asyncResponse, AtomicReference<List<Valintatulos>> vtsValintatulokset, Supplier<Void> mergeSuplier) {
        tilaResource.getValintatulokset(hakuOid, hakukohdeOid, vts -> {
                    LOG.info("Haetaan sijoittelusta valintatulokset");
                    vtsValintatulokset.set(vts);
                    mergeSuplier.get();
                },
                poikkeus -> logAndReturnError("valintatulosservice", asyncResponse, poikkeus)
        );
    }

    void fetchSijoittelu(@PathParam("hakuOid") String hakuOid, @PathParam("hakukohdeOid") String hakukohdeOid, @Suspended AsyncResponse asyncResponse, AtomicReference<HakukohdeDTO> hakukohde, Supplier<Void> mergeSuplier) {
        sijoitteluAsyncResource.getLatestHakukohdeBySijoittelu(hakuOid, hakukohdeOid,
                s -> {
                    LOG.info("Haetaan sijoittelusta hakukohteen tiedot");
                    hakukohde.set(s);
                    mergeSuplier.get();
                },
                poikkeus -> logAndReturnError("sijoittelupalvelu", asyncResponse, poikkeus)
        );
    }

    void fetchValinnanVaihes(@PathParam("hakukohdeOid") String hakukohdeOid, @Suspended AsyncResponse asyncResponse, AtomicReference<List<ValinnanVaiheJonoillaDTO>> valinnanvaiheet, Supplier<Void> mergeSuplier) {
        valintaperusteetAsyncResource.haeValinnanvaiheetHakukohteelle(hakukohdeOid,
                v -> {
                    LOG.info("Haetaan valinnanvaiheita");
                    valinnanvaiheet.set(v.stream().filter(vaihe -> vaihe.getValinnanVaiheTyyppi().equals(ValinnanVaiheTyyppi.TAVALLINEN)).collect(Collectors.toList()));
                    mergeSuplier.get();
                },
                poikkeus -> logAndReturnError("valintaperusteet", asyncResponse, poikkeus)
        );
    }

    void fetchHakemus(@PathParam("hakuOid") String hakuOid, @PathParam("hakukohdeOid") String hakukohdeOid, @Suspended AsyncResponse asyncResponse, AtomicReference<List<Hakemus>> hakemukset, Supplier<Void> mergeSuplier) {
        applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohdeOid).take(1).subscribe(
                h -> {
                    LOG.info("Haetaan hakemuksia");
                    hakemukset.set(h);
                    mergeSuplier.get();
                },
                poikkeus -> logAndReturnError("haku-app", asyncResponse, poikkeus)
        );
    }

    private void logAndReturnError(String appName, @Suspended AsyncResponse asyncResponse, Throwable poikkeus) {
        LOG.error("Erillishakuproxy -palvelukutsu epäonnistui " + appName + " :n virheeseen!", poikkeus);
        try {
            asyncResponse.resume(Response.serverError()
                    .entity("Erillishakuproxy -palvelukutsu epäonnistui + " + appName + " :n virheeseen: " + poikkeus.getMessage())
                    .build());
        } catch (Exception e) {
            LOG.error(appName + " virhe tuli yhtäaikaa timeoutin kanssa!", e);
        }
    }

    private void r(AsyncResponse asyncResponse, List<MergeValinnanvaiheDTO> msg) {
        try {
            asyncResponse.resume(Response.ok().header("Content-Type", "application/json").entity(msg).build());
        } catch (Throwable e) {
            LOG.error("Paluuarvon muodostos epäonnistui!", e);
        }
    }
}
