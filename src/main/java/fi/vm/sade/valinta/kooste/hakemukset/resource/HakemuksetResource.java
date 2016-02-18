package fi.vm.sade.valinta.kooste.hakemukset.resource;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.hakemukset.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.hakemukset.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.hakemukset.dto.ValintakoeDTO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import rx.Observable;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
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

    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @GET
    @Path("/valinnanvaihe")
    @Produces("application/json")
    @ApiOperation(value = "Pistesyötön vienti taulukkolaskentaan", response = HakemusDTO.class)
    public void hakemuksetValinnanvaiheelle(@QueryParam("hakuOid") String hakuOid, @QueryParam("valinnanvaiheOid") String valinnanvaiheOid, @Suspended AsyncResponse asyncResponse) {

        asyncResponse.setTimeout(10, TimeUnit.MINUTES);
        final Observable<List<Hakemus>> hakemuksetObservable = applicationAsyncResource.getApplicationsByOid(hakuOid, "");

        hakemuksetObservable.subscribe((hakemukset) -> {
            List<HakemusDTO> hakemusDTOs = hakemukset.stream().limit(100).map(hakemusTOHakemusDTO).collect(Collectors.toList());
            asyncResponse.resume(Response.ok(hakemusDTOs).build());
        });

    }

    private Function<Hakemus, HakemusDTO> hakemusTOHakemusDTO = new Function<Hakemus, HakemusDTO>() {
        @Override
        public HakemusDTO apply(Hakemus hakemus) {
            HakemusDTO hakemusDTO = new HakemusDTO();
            hakemusDTO.setHakemusOid(hakemus.getOid());
            hakemusDTO.setHenkiloOid(hakemus.getPersonOid());
            hakemusDTO.setEtunimet(hakemus.getAnswers().getHenkilotiedot().get("Etunimet"));
            hakemusDTO.setSukunimi(hakemus.getAnswers().getHenkilotiedot().get("Sukunimi"));
            hakemusDTO.setKutsumanimi(hakemus.getAnswers().getHenkilotiedot().get("Kutsumanimi"));
            hakemusDTO.setSahkoposti(hakemus.getAnswers().getHenkilotiedot().get("Sähköposti"));
            hakemusDTO.setKatuosoite(hakemus.getAnswers().getHenkilotiedot().get("lahiosoite"));
            hakemusDTO.setPostinumero(hakemus.getAnswers().getHenkilotiedot().get("Postinumero"));
            //TODO: Hae postitoimipaikka koodistosta
            hakemusDTO.setPostitoimipaikka("PUUTTUU");

            List<HakukohdeDTO> hakukohdeDTOs = hakemus.getAnswers().getHakutoiveet().entrySet().stream()
                    .filter(key -> key.getKey().startsWith("preference") && key.getKey().endsWith("-Koulutus-id"))
                    .map(key -> key.getValue())
                    .filter(value -> StringUtils.isNotEmpty(value))
                    .map(createHakukohdeDTO)
                    .collect(Collectors.toList());

            hakemusDTO.setHakukohteet(hakukohdeDTOs);

            return hakemusDTO;
        }
    };

    private Function<String, HakukohdeDTO> createHakukohdeDTO = new Function<String, HakukohdeDTO>() {
        @Override
        public HakukohdeDTO apply(String hakukohdeOid) {
            HakukohdeDTO hakukohdeDTO = new HakukohdeDTO();
            hakukohdeDTO.setHakukohdeOid(hakukohdeOid);
            ValintakoeDTO valintakoeDTO = new ValintakoeDTO();
            valintakoeDTO.setTunniste("sotelikoe2016k");
            hakukohdeDTO.setValintakokeet(new ArrayList<>());
            hakukohdeDTO.getValintakokeet().add(valintakoeDTO);
            return hakukohdeDTO;
        }
    };

}
