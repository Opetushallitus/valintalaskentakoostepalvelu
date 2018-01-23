package fi.vm.sade.valinta.kooste.external.resource.valintaperusteet;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeImportDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoDTO;
import rx.Observable;

import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

public interface ValintaperusteetAsyncResource {

    Observable<Map<String, List<ValintatapajonoDTO>>> haeValintatapajonotSijoittelulle (Collection<String> hakukohdeOids);

    Observable<List<ValintaperusteetHakijaryhmaDTO>> haeHakijaryhmat(String hakukohdeOid);

    // @GET /valintaperusteet-service/resources/hakukohde/haku/{}
    Observable<List<HakukohdeViiteDTO>> haunHakukohteet(String hakuOid);

    Observable<List<ValintaperusteetDTO>> haeValintaperusteet(String hakukohdeOid, Integer valinnanVaiheJarjestysluku);

    // @GET /valintaperusteet-service/resources/hakukohde/{hakukohdeOid}/ilmanlaskentaa/
    Observable<List<ValinnanVaiheJonoillaDTO>> haeIlmanlaskentaa(String hakukohdeOid);

    // @POST /valintaperusteet-service/resources/valintaperusteet/tuoHakukohde/
    Future<Response> tuoHakukohde(HakukohdeImportDTO hakukohde);

    Observable<List<ValintaperusteDTO>> findAvaimet(String hakukohdeOid);
    Observable<List<HakukohdeJaValintaperusteDTO>> findAvaimet(Collection<String> hakukohdeOids);

    Observable<List<ValintaperusteetDTO>> valintaperusteet(String valinnanvaiheOid);

    Observable<List<ValintakoeDTO>> haeValintakokeetHakukohteelle(String hakukohdeOid);

    // @POST /valintaperusteet-service/resources/hakukohde/valintakoe
    Future<List<HakukohdeJaValintakoeDTO>> haeValintakokeetHakukohteille(Collection<String> hakukohdeOids);

    Observable<List<HakukohdeJaValintakoeDTO>> haeValintakokeetHakutoiveille(Collection<String> hakukohdeOids);

    // @GET /valintaperusteet-service/resources/valinnanvaihe/{oid}/hakukohteet
    Observable<Set<String>> haeHakukohteetValinnanvaiheelle(String oid);
}
