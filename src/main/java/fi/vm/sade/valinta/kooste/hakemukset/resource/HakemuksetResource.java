package fi.vm.sade.valinta.kooste.hakemukset.resource;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.hakemukset.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.hakemukset.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.security.AuthorityCheckService;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fi.vm.sade.auditlog.valintaperusteet.LogMessage.builder;
import static fi.vm.sade.valinta.kooste.KoosteAudit.AUDIT;
import static java.util.Arrays.asList;


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
    private ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource;

    @Autowired
    private KoodistoCachedAsyncResource koodistoCachedAsyncResource;

    @Autowired
    private AuthorityCheckService authorityCheckService;

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
            AUDIT.log(builder()
                    .id(KoosteAudit.username())
                    .valinnanvaiheOid(valinnanvaiheOid)
                    .hakuOid(hakuOid)
                    .setOperaatio(ValintaperusteetOperation.VALINNANVAIHEEN_HAKEMUKSET_HAKU)
                    .build());
            LOG.warn("Aloitetaan hakemusten listaaminen valinnenvaiheelle {} haussa {}", valinnanvaiheOid, hakuOid);

            authorityCheckService.getAuthorityCheckForRoles(
                    asList("ROLE_APP_HAKEMUS_READ_UPDATE", "ROLE_APP_HAKEMUS_READ", "ROLE_APP_HAKEMUS_CRUD", "ROLE_APP_HAKEMUS_LISATIETORU", "ROLE_APP_HAKEMUS_LISATIETOCRUD"),
                    authCheck -> {
                        valintaperusteetAsyncResource.haeHakukohteetValinnanvaiheelle(valinnanvaiheOid)
                        .flatMap(hakukohdeOidit -> {
                            LOG.info("Löydettiin {} hakukohdetta", hakukohdeOidit.size());
                            return applicationAsyncResource.getApplicationsByOidsWithPOST(hakuOid, hakukohdeOidit);
                        })
                        .subscribe(hakemukset -> {
                            LOG.info("Löydettiin {} hakemusta", hakemukset.size());
                            List<String> hakutoiveet = collect(hakemukset);
                            Observable<List<HakukohdeJaValintakoeDTO>> valintakokeetHakutoiveille = valintaperusteetAsyncResource.haeValintakokeetHakutoiveille(hakutoiveet);
                            Observable<List<ValintakoeOsallistuminenDTO>> valintakoeOsallistumisetHakutoiveille = valintalaskentaValintakoeAsyncResource.haeHakutoiveille(hakutoiveet);
                            Observable.combineLatest(valintakokeetHakutoiveille, valintakoeOsallistumisetHakutoiveille, (hakutoiveidenValintakokeet, hakutoiveidenValintakoeOsallistumiset) -> {
                                Map<String, HakukohdeJaValintakoeDTO> valintakoeDTOMap = hakutoiveidenValintakokeet.stream().collect(Collectors.toMap(HakukohdeJaValintakoeDTO::getHakukohdeOid, hh -> hh));
                                Map<String, List<ValintakoeOsallistuminenDTO>> osallistuminenDTOMap = hakutoiveidenValintakoeOsallistumiset.stream().collect(Collectors.toMap(ValintakoeOsallistuminenDTO::getHakemusOid, Arrays::asList, (h0, h1) -> Lists.newArrayList(Iterables.concat(h0,h1))));
                                return hakemukset.stream().map(hakemus -> {
                                    if (osallistuminenDTOMap.get(hakemus.getOid()) == null) {
                                        LOG.error("Listing hakemus for valinnanvaihe {} and haku {} failed", valinnanvaiheOid, hakuOid);
                                        asyncResponse.cancel();
                                    }
                                    Set<String> kutsututValintakokeet = osallistuminenDTOMap.get(hakemus.getOid()).stream()
                                            .flatMap(x -> x.getHakutoiveet().stream())
                                            .filter(x -> x != null)
                                            .flatMap(x -> x.getValinnanVaiheet().stream())
                                            .filter(x -> x != null)
                                            .flatMap(x -> x.getValintakokeet().stream())
                                            .filter(x -> x != null)
                                            .map(x -> x.getValintakoeOid())
                                            .collect(Collectors.toSet());

                                    final List<String> hakutoiveOids = new HakemusWrapper(hakemus).getHakutoiveOids();
                                    final List<HakukohdeJaValintakoeDTO> hakukohteet = hakutoiveOids
                                            .stream()
                                            .map(x -> valintakoeDTOMap.get(x))
                                            .filter(x -> x != null)
                                            .map(hakukohdeJaValintakoe -> {
                                                final List<ValintakoeDTO> valintakokeet = hakukohdeJaValintakoe.getValintakoeDTO();
                                                final List<ValintakoeDTO> filteredValintakokeet = valintakokeet.stream().filter(valintakoe -> {
                                                    if (valintakoe.getKutsutaankoKaikki()) {
                                                        return true;
                                                    } else if (kutsututValintakokeet.contains(valintakoe.getOid())) {
                                                        return true;
                                                    }
                                                    return false;
                                                }).collect(Collectors.toList());
                                                return new HakukohdeJaValintakoeDTO(hakukohdeJaValintakoe.getHakukohdeOid(), filteredValintakokeet);
                                            })
                                            .filter(hakukohdeJaValintakoeDTO -> !hakukohdeJaValintakoeDTO.getValintakoeDTO().isEmpty())
                                            .collect(Collectors.toList());
                                    return hakemusToHakemusDTO(hakemus, hakukohteet);
                                }).collect(Collectors.toList());
                            }).subscribe(hakemusDTOs -> {
                                asyncResponse.resume(Response.ok(hakemusDTOs).build());
                            });
                        });
                    },
                    poikkeus -> {
                        LOG.error("Unable to get hakukohteet from tarjonta", poikkeus);
                        asyncResponse.resume(Response.serverError().entity(poikkeus).build());
                    });
        } catch (Exception e) {
            LOG.error("Listing hakemus for valinnanvaihe {} and haku {} failed", valinnanvaiheOid, hakuOid, e);
            asyncResponse.cancel();
        }
    }

    private List<String> collect(List<Hakemus> hakemukset) {
        return hakemukset.stream().flatMap(h -> new HakemusWrapper(h).getHakutoiveOids().stream()).collect(Collectors.toList());
    }

    private HakemusDTO hakemusToHakemusDTO(Hakemus hakemus, List<HakukohdeJaValintakoeDTO> valintakoeDTOs) {
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
        hakemusDTO.setHakukohteet(valintakoeDTOs.stream().map(vk -> new HakukohdeDTO(vk.getHakukohdeOid(), vk.getValintakoeDTO().stream().map(vv -> new fi.vm.sade.valinta.kooste.hakemukset.dto.ValintakoeDTO(vv.getSelvitettyTunniste())).collect(Collectors.toList()))).collect(Collectors.toList()));
        return hakemusDTO;
    }
}
