package fi.vm.sade.valinta.kooste.proxy.resource.suoritukset;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusHakija;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
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
import rx.observables.BlockingObservable;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller("SuorituksenArvosanatProxyResource")
@Path("/proxy/suoritukset")
@PreAuthorize("isAuthenticated()")
@Api(value = "/proxy/suoritukset", description = "Käyttöliittymäkutsujen välityspalvelin suoritusrekisteriin")
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
        resolveHakemusDTO(hakuOid, opiskeljaOid, applicationAsyncResource.getApplication(hakemusOid), true, hakemusDTO -> {
            asyncResponse.resume(Response
                    .ok()
                    .header("Content-Type", "application/json")
                    .entity(getAvainArvoMap(hakemusDTO))
                    .build());
        }, poikkeus -> {
            LOG.error("OppijanSuorituksetProxyResource exception", poikkeus);
            asyncResponse.resume(Response.serverError().entity(poikkeus.getMessage()).build());
        });
    }


    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @POST
    @Path("/suorituksetByHakemusOids/hakuOid/{hakuOid}")
    @Consumes("application/json")
    @ApiOperation(consumes = "application/json", value = "Hakemukset suoritustietoineen tietylle haulle", response = Response.class)
    public void getSuoritukset(
            @PathParam("hakuOid") String hakuOid,
            @DefaultValue("false") @QueryParam("fetchEnsikertalaisuus") Boolean fetchEnsikertalaisuus,
            List<String> hakemusOids,
            @Suspended final AsyncResponse asyncResponse) {

        asyncResponse.setTimeout(2L, TimeUnit.MINUTES);
        asyncResponse.setTimeoutHandler(handler -> {
            LOG.error("suorituksetByOpiskeljaOid proxy -palvelukutsu on aikakatkaistu: /suorituksetByOpiskeljaOid/{hakuOid}", hakuOid);
            handler.resume(Response.serverError()
                    .entity("Suoritus proxy -palvelukutsu on aikakatkaistu")
                    .build());
        });

        resolveHakemusDTOs(hakuOid, hakemusOids, fetchEnsikertalaisuus,
                (hakemusDTOs -> {
                    List<Map<String, String>> listOfMaps = hakemusDTOs.stream().map(this::getAvainArvoMap).collect(Collectors.toList());

                    Response resp = Response.ok()
                                    .header("Content-Type", "application/json")
                                    .entity(listOfMaps)
                                    .build();

                    asyncResponse.resume(resp);
                }),
                (exception -> {
                    LOG.error("OppijanSuorituksetProxyResource exception", exception);
                    asyncResponse.resume(Response.serverError().entity(exception.getMessage()).build());
                })
        );
    }

    /**
     *
     @deprecated Use the one with the fixed path (opiskelijaOid instead of opiskeljaOid) {@link #getSuoritukset(String, String, Boolean, Hakemus, AsyncResponse)} ()}
     */
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @POST
    @Path("/suorituksetByOpiskelijaOid/hakuOid/{hakuOid}/opiskeljaOid/{opiskeljaOid}")
    @Deprecated
    public void getSuorituksetOld(
            @PathParam("hakuOid") String hakuOid,
            @PathParam("opiskeljaOid") String opiskeljaOid,
            @DefaultValue("false") @QueryParam("fetchEnsikertalaisuus") Boolean fetchEnsikertalaisuus,
            Hakemus hakemus,
            @Suspended final AsyncResponse asyncResponse) {
        getSuoritukset(hakuOid, opiskeljaOid, fetchEnsikertalaisuus, hakemus, asyncResponse);
    }

    /*
    Same as above except with the typo on path fixed (opiskeljaOid -> opiskelijaOid)
     */
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @POST
    @Path("/suorituksetByOpiskelijaOid/hakuOid/{hakuOid}/opiskelijaOid/{opiskelijaOid}")
    public void getSuoritukset(
            @PathParam("hakuOid") String hakuOid,
            @PathParam("opiskelijaOid") String opiskelijaOid,
            @DefaultValue("false") @QueryParam("fetchEnsikertalaisuus") Boolean fetchEnsikertalaisuus,
            Hakemus hakemus,
            @Suspended final AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(2L, TimeUnit.MINUTES);
        asyncResponse.setTimeoutHandler(handler -> {
            LOG.error("suorituksetByOpiskelijaOid proxy -palvelukutsu on aikakatkaistu: /suorituksetByOpiskelijaOid/{oid}", opiskelijaOid);
            handler.resume(Response.serverError()
                    .entity("Suoritus proxy -palvelukutsu on aikakatkaistu")
                    .build());
        });
        resolveHakemusDTO(hakuOid, opiskelijaOid, Observable.just(hakemus), fetchEnsikertalaisuus, hakemusDTO -> {
            asyncResponse.resume(Response
                    .ok()
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(getAvainArvoMap(hakemusDTO))
                    .build());
        }, poikkeus -> {
            LOG.error("OppijanSuorituksetProxyResource exception", poikkeus);
            asyncResponse.resume(Response.serverError().entity(poikkeus.getMessage()).build());
        });
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/suorituksetByOpiskelijaOid/hakuOid/{hakuOid}")
    public void getSuorituksetForOpiskelijas(
            @PathParam("hakuOid") String hakuOid,
            final List<HakemusHakija> allHakemus,
            @DefaultValue("false") @QueryParam("fetchEnsikertalaisuus") Boolean fetchEnsikertalaisuus,
            @Suspended final AsyncResponse asyncResponse) {

        asyncResponse.setTimeout(2L, TimeUnit.MINUTES);
        asyncResponse.setTimeoutHandler(handler -> {
            LOG.error("suorituksetByOpiskeljaOid proxy -palvelukutsu on aikakatkaistu");
            handler.resume(Response.serverError()
                    .entity("Suoritus proxy -palvelukutsu on aikakatkaistu")
                    .build());
        });

        if (allHakemus == null || allHakemus.isEmpty()) {
            asyncResponse.resume(Response.status(Response.Status.NO_CONTENT).build());
            return;
        }

        final Map<String, Map<String, String>> allData = new HashMap<>();
        final BlockingObservable<HakuV1RDTO> hakuObservable = tarjontaAsyncResource.haeHaku(hakuOid).toBlocking();
        HakuV1RDTO haku = hakuObservable.first();

        if (haku == null) {
            String msg = String.format("Hakua %s ei löytynyt", hakuOid);
            asyncResponse.resume(Response
                    .noContent()
                    .entity(msg)
                    .build());
            return;
        }

         Action1<Throwable> exceptionConsumer = (Throwable poikkeus) -> {
            LOG.error("OppijanSuorituksetProxyResource exception", poikkeus);
            asyncResponse.resume(Response.serverError().entity(poikkeus.getMessage()).build());
        };

        LOG.info("Hae suoritukset {} hakemukselle", allHakemus.size());

        List<String> hakemusOids = allHakemus.stream().map(h -> h.getHakemus().getOid()).collect(Collectors.toList());

        resolveHakemusDTOs(hakuOid, hakemusOids, fetchEnsikertalaisuus, hakemusDTOs -> {
            hakemusDTOs.forEach(hakemusDTO -> {
                Map<String, String> data = getAvainArvoMap(hakemusDTO);
                allData.put(hakemusDTO.getHakijaOid(), data);
            });
        }, exceptionConsumer);

        LOG.info("Haettiin {} hakemukselle {} suoritustietoa", allHakemus.size(), allData.size());

        asyncResponse.resume(Response
                .ok()
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(allData)
                .build());
    }

    private Map<String,String> getAvainArvoMap(HakemusDTO hakemusDTO) {
        return hakemusDTO.getAvaimet().stream().map(a ->
                a.getAvain().endsWith("_SUORITETTU")
                        ? new AvainArvoDTO(a.getAvain().replaceFirst("_SUORITETTU", ""), "S")
                        : a
        ).collect(Collectors.toMap(AvainArvoDTO::getAvain, AvainArvoDTO::getArvo));
    }

    private void resolveHakemusDTO(HakuV1RDTO haku, Observable<ParametritDTO> parametritDTOObservable, String opiskelijaOid, Observable<Hakemus> hakemusObservable, Boolean fetchEnsikertalaisuus,
                                   Action1<HakemusDTO> hakemusDTOConsumer, Action1<Throwable> throwableConsumer) {
        hakemusObservable.doOnError(throwableConsumer);

        Observable<Oppija> suorituksetByOppija = fetchEnsikertalaisuus ?
                suoritusrekisteriAsyncResource.getSuorituksetByOppija(opiskelijaOid, haku.getOid()).doOnError(throwableConsumer) :
                suoritusrekisteriAsyncResource.getSuorituksetWithoutEnsikertalaisuus(opiskelijaOid).doOnError(throwableConsumer);

        Observable.combineLatest(suorituksetByOppija, hakemusObservable, parametritDTOObservable,
                (oppija, hakemus, ohjausparametrit) -> HakemuksetConverterUtil.muodostaHakemuksetDTO(
                        haku,
                        "",
                        Collections.singletonList(hakemus),
                        Collections.singletonList(oppija),
                        ohjausparametrit,
                        fetchEnsikertalaisuus).get(0)
        ).subscribe(hakemusDTOConsumer, throwableConsumer);
    }

    private void resolveHakemusDTO(String hakuOid, String opiskeljaOid, Observable<Hakemus> hakemusObservable, Boolean fetchEnsikertalaisuus,
                                   Action1<HakemusDTO> hakemusDTOConsumer, Action1<Throwable> throwableConsumer) {
        hakemusObservable.doOnError(throwableConsumer);
        Observable<HakuV1RDTO> hakuObservable = tarjontaAsyncResource.haeHaku(hakuOid).doOnError(throwableConsumer);
        Observable<Oppija> suorituksetByOppija = fetchEnsikertalaisuus ?
                suoritusrekisteriAsyncResource.getSuorituksetByOppija(opiskeljaOid, hakuOid).doOnError(throwableConsumer) :
                suoritusrekisteriAsyncResource.getSuorituksetWithoutEnsikertalaisuus(opiskeljaOid);
        Observable<ParametritDTO> parametritDTOObservable = ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid).doOnError(throwableConsumer);
        Observable.combineLatest(hakuObservable, suorituksetByOppija, hakemusObservable, parametritDTOObservable,
                (haku, suoritukset, hakemus, ohjausparametrit) -> HakemuksetConverterUtil.muodostaHakemuksetDTO(
                        haku,
                        "",
                        Collections.singletonList(hakemus),
                        Collections.singletonList(suoritukset),
                        ohjausparametrit,
                        fetchEnsikertalaisuus).get(0)
        ).subscribe(hakemusDTOConsumer, throwableConsumer);
    }

    /**
     * Fetch and combine data of Hakemus and Suoritus for a single Haku
     *
     * @param hakuOid Used for retrieving Haku from Tarjonta
     * @param hakemusOids Used to limit Hakemukset from Hakuapp
     * @param fetchEnsikertalaisuus Boolean flag if 'ensikertalaisuus' should be fetched
     * @param onNext Action1 Handler for successful operation taking a List<HakemusDTO>
     * @param onError Action1 Handler for exceptions
     */
    private void resolveHakemusDTOs(String hakuOid,
                                    List<String> hakemusOids,
                                    Boolean fetchEnsikertalaisuus,
                                    Action1<List<HakemusDTO>> onNext,
                                    Action1<Throwable> onError) {

        Observable<HakuV1RDTO>    hakuObservable           = tarjontaAsyncResource.haeHaku(hakuOid).doOnError(onError);
        Observable<List<Hakemus>> hakemuksetObservable     = applicationAsyncResource.getApplicationsByHakemusOids(hakemusOids).doOnError(onError);
        Observable<ParametritDTO> parametritObservable     = ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid).doOnError(onError);

        // Fetch Oppija (suoritusdata) for each personOid in hakemukset
        Observable<List<String>>  opiskelijaOidsObservable = hakemuksetObservable.flatMap(Observable::from).map(Hakemus::getPersonOid).toList();
        Observable<List<Oppija>>  suorituksetObservable    = opiskelijaOidsObservable.flatMap(os -> {
            if (fetchEnsikertalaisuus) {
                return suoritusrekisteriAsyncResource.getSuorituksetByOppijas(os, hakuOid).doOnError(onError);
            } else {
                return suoritusrekisteriAsyncResource.getSuorituksetWithoutEnsikertalaisuus(os).doOnError(onError);
            }
        });

        /**
         * Combine observables using zip
         *
         * When each have a value merge the data using a converter and return a list of HakemusDTOs
         */
        Observable.zip(hakuObservable, suorituksetObservable, hakemuksetObservable, parametritObservable,
                (haku, suoritukset, hakemukset, parametrit) -> createHakemusDTOs(haku, suoritukset, hakemukset, parametrit, fetchEnsikertalaisuus)
        ).subscribe(onNext, onError);
    }

    private List<HakemusDTO> createHakemusDTOs (HakuV1RDTO haku,
                                                List<Oppija> suoritukset,
                                                List<Hakemus> hakemukset,
                                                ParametritDTO parametrit,
                                                Boolean fetchEnsikertalaisuus) {

        return HakemuksetConverterUtil.muodostaHakemuksetDTO(haku, "", hakemukset, suoritukset, parametrit, fetchEnsikertalaisuus);
    }


}
