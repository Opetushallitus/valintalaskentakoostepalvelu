package fi.vm.sade.valinta.kooste.proxy.resource.suoritukset;

import com.google.common.collect.Maps;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.function.SynkronoituLaskuri;
import fi.vm.sade.valinta.kooste.util.Converter;
import fi.vm.sade.valinta.kooste.util.PoikkeusKasittelijaSovitin;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.valintalaskenta.util.HakemuksetConverterUtil;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
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
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Controller("SuorituksenArvosanatProxyResource")
@Path("/proxy/suoritukset")
@PreAuthorize("isAuthenticated()")
public class OppijanSuorituksetProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(OppijanSuorituksetProxyResource.class);

    @Autowired
    private SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;

    @Autowired
    private OhjausparametritAsyncResource ohjausparametritAsyncResource;

    @Autowired
    private ApplicationAsyncResource applicationAsyncResource;

    @Autowired
    private TarjontaAsyncResource tarjontaAsyncResource;

    @GET
    @Path("/suorituksetByOpiskelijaOid/hakuOid/{hakuOid}/opiskeljaOid/{opiskeljaOid}/hakemusOid/{hakemusOid}")
    @Consumes("application/json")
    public void getSuoritukset(
            @PathParam("hakuOid") String hakuOid,
            @PathParam("opiskeljaOid") String opiskeljaOid,
            @PathParam("hakemusOid") String hakemusOid,
            @Suspended final AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(1L, TimeUnit.MINUTES);
        asyncResponse.setTimeoutHandler(handler -> {
            LOG.error("suorituksetByOpiskeljaOid proxy -palvelukutsu on aikakatkaistu: /suorituksetByOpiskeljaOid/{oid}", opiskeljaOid);
            handler.resume(Response.serverError()
                    .entity("Suoritus proxy -palvelukutsu on aikakatkaistu")
                    .build());
        });

        try {
            PoikkeusKasittelijaSovitin poikkeuskasittelija = new PoikkeusKasittelijaSovitin(poikkeus -> {
                LOG.error("", poikkeus);
                asyncResponse.resume(Response.serverError().entity(poikkeus.getMessage()).build());
            });

            AtomicReference<Oppija> oppijaRef = new AtomicReference<>();
            AtomicReference<ParametritDTO> parametriRef = new AtomicReference<>();
            AtomicReference<Hakemus> hakemusRef = new AtomicReference<>();
            AtomicReference<HakuV1RDTO> tarjontaRef = new AtomicReference<>();

            SynkronoituLaskuri laskuri = SynkronoituLaskuri.builder()
                    .setLaskurinAlkuarvo(4)
                    .setSynkronoituToiminto(
                            () -> {
                                HakemusDTO hakemusDTO = HakemuksetConverterUtil.muodostaHakemuksetDTO(
                                        tarjontaRef.get(),
                                        "",
                                        Collections.singletonList(hakemusRef.get()),
                                        Collections.singletonList(oppijaRef.get()),
                                        parametriRef.get()).get(0);
                                asyncResponse.resume(Response
                                        .ok()
                                        .header("Content-Type", "application/json")
                                        .entity(hakemusDTO.getAvaimet().stream()
                                                .map(a -> a.getAvain().endsWith("_SUORITETTU") ? new AvainArvoDTO(a.getAvain().replaceFirst("_SUORITETTU", ""), "S") : a)
                                                .collect(Collectors.toMap(AvainArvoDTO::getAvain, AvainArvoDTO::getArvo)))
                                        .build());
                            }
                    ).build();

            tarjontaAsyncResource.haeHaku(hakuOid).subscribe(
                    tarjonta -> {
                        tarjontaRef.set(tarjonta);
                        laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                    },
                    poikkeuskasittelija);

            applicationAsyncResource.getApplication(hakemusOid, hakemus -> {
                hakemusRef.set(hakemus);
                laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
            }, poikkeuskasittelija);

            ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid, parametritDTO -> {
                        parametriRef.set(parametritDTO);
                        laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                    },
                    poikkeuskasittelija
            );

            suoritusrekisteriAsyncResource.getSuorituksetByOppija(opiskeljaOid, opiskelija -> {
                oppijaRef.set(opiskelija);
                laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
            }, poikkeuskasittelija);
        } catch (Throwable t) {
            LOG.error("", t);
            asyncResponse.resume(Response.serverError().entity(t.getMessage()).build());
        }
    }
}
