package fi.vm.sade.valinta.kooste.hakemukset.resource;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.hakemukset.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.hakemukset.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.hakemukset.dto.ValintakoeDTO;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import rx.Observable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller("HakemuksetResource")
@Path("hakemukset")
@PreAuthorize("isAuthenticated()")
@Api(value = "/hakemukset", description = "Hakemusten hakeminen")
public class HakemuksetResource {

    private static final Logger LOG = LoggerFactory.getLogger(HakemuksetResource.class);

    @Autowired
    private ApplicationAsyncResource applicationAsyncResource;

    @Autowired
    private ValintaperusteetAsyncResource valintaperusteetAsyncResource;

    @Autowired
    private KoodistoCachedAsyncResource koodistoCachedAsyncResource;

    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @GET
    @Path("/valinnanvaihe")
    @Produces("application/json")
    @ApiOperation(value = "Valinnanvaiheen hakemusten listaus", response = HakemusDTO.class)
    public void hakemuksetValinnanvaiheelle(@QueryParam("hakuOid") String hakuOid, @QueryParam("valinnanvaiheOid") String valinnanvaiheOid, @Suspended AsyncResponse asyncResponse) {
        try {
            Preconditions.checkNotNull(hakuOid);
            Preconditions.checkNotNull(valinnanvaiheOid);
            asyncResponse.setTimeout(10, TimeUnit.MINUTES);
            Set<HakemusDTO> hakemusDTOs = new HashSet<>();

            LOG.warn("Aloitetaan hakemusten listaaminen valinnenvaiheelle {} haussa {}", valinnanvaiheOid, hakuOid);
            final Observable<Set<String>> hakukohdeOiditObservable = valintaperusteetAsyncResource.haeHakukohteetValinnanvaiheelle(valinnanvaiheOid);
            hakukohdeOiditObservable.subscribe(hakukohdeOidit -> {
                List<String> hakukohteet = hakukohdeOidit.stream().collect(Collectors.toList());
                LOG.info("Löydettiin {} hakukohdetta", hakukohteet.size());
                for (List<String> sublist : Lists.partition(hakukohteet, 10)) {
                    LOG.info("Käsitellään {} hakukohdetta", sublist.size());
                    final Observable<List<Hakemus>> hakemuksetObservable = applicationAsyncResource.getApplicationsByOids(hakuOid, sublist);
                    hakemuksetObservable.subscribe(hakemukset -> {
                        LOG.info("Löydettiin {} hakemusta", hakemukset.size());
                        List<HakemusDTO> dtos = hakemukset.stream().map(hakemusTOHakemusDTO).collect(Collectors.toList());
                        hakemusDTOs.addAll(dtos);
                    });
                }
                asyncResponse.resume(Response.ok(hakemusDTOs).build());
            });
        } catch (Exception e) {
            LOG.error("Listing hakemus for valinnanvaihe {} and haku {} failed", valinnanvaiheOid, hakuOid, e);
            asyncResponse.cancel();
        }
    }

    private Function<Hakemus, HakemusDTO> hakemusTOHakemusDTO = new Function<Hakemus, HakemusDTO>() {
        @Override
        public HakemusDTO apply(Hakemus hakemus) {
            Map<String, Koodi> postCodes = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI);
            HakemusDTO hakemusDTO = new HakemusDTO();
            hakemusDTO.setHakemusOid(hakemus.getOid());
            hakemusDTO.setHenkiloOid(hakemus.getPersonOid());
            hakemusDTO.setEtunimet(hakemus.getAnswers().getHenkilotiedot().get("Etunimet"));
            hakemusDTO.setSukunimi(hakemus.getAnswers().getHenkilotiedot().get("Sukunimi"));
            hakemusDTO.setKutsumanimi(hakemus.getAnswers().getHenkilotiedot().get("Kutsumanimi"));
            hakemusDTO.setSahkoposti(hakemus.getAnswers().getHenkilotiedot().get("Sähköposti"));
            hakemusDTO.setKatuosoite(hakemus.getAnswers().getHenkilotiedot().get("lahiosoite"));

            String postinumero = hakemus.getAnswers().getHenkilotiedot().get("Postinumero");
            hakemusDTO.setPostinumero(postinumero);
            hakemusDTO.setPostitoimipaikka(KoodistoCachedAsyncResource.haeKoodistaArvo(
                    postCodes.get(postinumero),
                    KieliUtil.SUOMI,
                    postinumero));

            List<HakukohdeDTO> hakukohdeDTOs = hakemus.getAnswers().getHakutoiveet().entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith("preference") && entry.getKey().endsWith("-Koulutus-id"))
                    .map(entry -> entry.getValue())
                    .filter(value -> StringUtils.isNotEmpty(value))
                    .map(createHakukohdeDTO)
                    .collect(Collectors.toList());

            hakemusDTO.setHakukohteet(hakukohdeDTOs);

            return hakemusDTO;
        }
    };

    private Function<String, HakukohdeDTO> createHakukohdeDTO = hakukohdeOid -> {
        HakukohdeDTO hakukohdeDTO = new HakukohdeDTO();
        hakukohdeDTO.setHakukohdeOid(hakukohdeOid);
        ValintakoeDTO valintakoeDTO = new ValintakoeDTO();
        valintakoeDTO.setTunniste("sotelikoe2016k");
        hakukohdeDTO.setValintakokeet(new ArrayList<>());
        hakukohdeDTO.getValintakokeet().add(valintakoeDTO);
        return hakukohdeDTO;
    };

}
