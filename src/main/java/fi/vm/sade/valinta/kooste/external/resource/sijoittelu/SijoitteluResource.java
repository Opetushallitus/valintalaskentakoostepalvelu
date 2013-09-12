package fi.vm.sade.valinta.kooste.external.resource.sijoittelu;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.SijoitteluajoDTO;

// SijoitteluResource
//@Path("sijoittelu")
@Component
public interface SijoitteluResource {

    static final String LATEST = "latest";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sijoittelu/{hakuOid}/sijoitteluajo/{sijoitteluajoId}")
    SijoitteluajoDTO getSijoitteluajo(@PathParam("hakuOid") String hakuOid,
            @PathParam("sijoitteluajoId") String sijoitteluajoId);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sijoittelu/{hakuOid}/sijoitteluajo/{sijoitteluajoId}/hakukohde/{hakukohdeOid}")
    HakukohdeDTO getHakukohdeBySijoitteluajo(@PathParam("hakuOid") String hakuOid,
            @PathParam("sijoitteluajoId") String sijoitteluajoId, @PathParam("hakukohdeOid") String hakukohdeOid);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sijoittelu/{hakuOid}/sijoitteluajo/{sijoitteluajoId}/hakemus/{hakemusOid}")
    List<HakemusDTO> getHakemusBySijoitteluajo(@PathParam("hakuOid") String hakuOid,
            @PathParam("sijoitteluajoId") String sijoitteluajoId, @PathParam("hakemusOid") String hakemusOid);
}
