package fi.vm.sade.valinta.kooste.proxy.resource.suoritukset;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.util.HakemuksetConverterUtil;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import rx.Observable;
import rx.functions.Action1;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
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

    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @GET
    @Path("/suorituksetByOpiskelijaOid/hakuOid/{hakuOid}/opiskeljaOid/{opiskeljaOid}/hakemusOid/{hakemusOid}")
    public void getSuoritukset(
            @PathParam("hakuOid") String hakuOid,
            @PathParam("opiskeljaOid") String opiskeljaOid,
            @PathParam("hakemusOid") String hakemusOid,
            @Suspended final AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(2L, TimeUnit.MINUTES);
        asyncResponse.setTimeoutHandler(handler -> {
            LOG.error("suorituksetByOpiskeljaOid proxy -palvelukutsu on aikakatkaistu: /suorituksetByOpiskeljaOid/{oid}", opiskeljaOid);
            handler.resume(Response.serverError()
                    .entity("Suoritus proxy -palvelukutsu on aikakatkaistu")
                    .build());
        });
        resolveHakemusDTO(hakuOid, opiskeljaOid, applicationAsyncResource.getApplication(hakemusOid), hakemusDTO -> {
            asyncResponse.resume(Response
                    .ok()
                    .header("Content-Type", "application/json")
                    .entity(hakemusDTO.getAvaimet().stream()
                            .map(a -> a.getAvain().endsWith("_SUORITETTU") ? new AvainArvoDTO(a.getAvain().replaceFirst("_SUORITETTU", ""), "S") : a)
                            .collect(Collectors.toMap(AvainArvoDTO::getAvain, AvainArvoDTO::getArvo)))
                    .build());
        }, poikkeus -> {
            LOG.error("OppijanSuorituksetProxyResource exception", poikkeus);
            asyncResponse.resume(Response.serverError().entity(poikkeus.getMessage()).build());
        });
    }
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @POST
    @Path("/suorituksetByOpiskelijaOid/hakuOid/{hakuOid}/opiskeljaOid/{opiskeljaOid}")
    public void getSuoritukset(
            @PathParam("hakuOid") String hakuOid,
            @PathParam("opiskeljaOid") String opiskeljaOid,
            Hakemus hakemus,
            @Suspended final AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(2L, TimeUnit.MINUTES);
        asyncResponse.setTimeoutHandler(handler -> {
            LOG.error("suorituksetByOpiskeljaOid proxy -palvelukutsu on aikakatkaistu: /suorituksetByOpiskeljaOid/{oid}", opiskeljaOid);
            handler.resume(Response.serverError()
                    .entity("Suoritus proxy -palvelukutsu on aikakatkaistu")
                    .build());
        });
        resolveHakemusDTO(hakuOid, opiskeljaOid, Observable.just(hakemus), hakemusDTO -> {
            asyncResponse.resume(Response
                    .ok()
                    .header("Content-Type", "application/json")
                    .entity(hakemusDTO.getAvaimet().stream()
                            .map(a -> a.getAvain().endsWith("_SUORITETTU") ? new AvainArvoDTO(a.getAvain().replaceFirst("_SUORITETTU", ""), "S") : a)
                            .collect(Collectors.toMap(AvainArvoDTO::getAvain, AvainArvoDTO::getArvo)))
                    .build());
        }, poikkeus -> {
            LOG.error("OppijanSuorituksetProxyResource exception", poikkeus);
            asyncResponse.resume(Response.serverError().entity(poikkeus.getMessage()).build());
        });
    }
    private void resolveHakemusDTO(String hakuOid, String opiskeljaOid, Observable<Hakemus> hakemusObservable, Action1<HakemusDTO> hakemusDTOConsumer, Action1<Throwable> throwableConsumer) {
        long start = System.currentTimeMillis();
        hakemusObservable.doOnError(throwableConsumer);
        Observable<HakuV1RDTO> hakuObservable = tarjontaAsyncResource.haeHaku(hakuOid)
                .doOnEach(a -> { LOG.info(String.format("%s alkanut suorituksetByOpiskelijaOid tarjonta kutsu kesti %s ms", start, (System.currentTimeMillis() - start))); })
                .doOnError(throwableConsumer);
        Observable<Oppija> suorituksetByOppija = suoritusrekisteriAsyncResource.getSuorituksetByOppija(opiskeljaOid, hakuOid)
                .doOnEach(a -> { LOG.info(String.format("%s alkanut suorituksetByOpiskelijaOid suoritusrekisteri kutsu kesti %s ms", start, (System.currentTimeMillis() - start))); })
                .doOnError(throwableConsumer);
        Observable<ParametritDTO> parametritDTOObservable = ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid)
                .doOnEach(a -> { LOG.info(String.format("%s alkanut suorituksetByOpiskelijaOid ohjausparametrit kutsu kesti %s ms", start, (System.currentTimeMillis() - start))); })
                .doOnError(throwableConsumer);
        Observable.combineLatest(hakuObservable, suorituksetByOppija, hakemusObservable, parametritDTOObservable,
                (haku, suoritukset, hakemus, ohjausparametrit) -> HakemuksetConverterUtil.muodostaHakemuksetDTO(
                        haku,
                        "",
                        Collections.singletonList(hakemus),
                        Collections.singletonList(suoritukset),
                        ohjausparametrit).get(0)
        ).subscribe(hakemusDTOConsumer, throwableConsumer);
    }
}
