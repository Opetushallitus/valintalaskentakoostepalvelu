package fi.vm.sade.valinta.kooste.external.resource.valintaperusteet;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.ws.rs.core.Response;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeImportDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import rx.Observable;

public interface ValintaperusteetAsyncResource {

    // @GET /valintaperusteet-service/resources/valintaperusteet/hakijaryhma/{}
    @Deprecated
    Peruutettava haeHakijaryhmat(String hakukohdeOid, Consumer<List<ValintaperusteetHakijaryhmaDTO>> callback, Consumer<Throwable> failureCallback);

    Observable<List<ValintaperusteetHakijaryhmaDTO>> haeHakijaryhmat(String hakukohdeOid);

    // @GET /valintaperusteet-service/resources/hakukohde/haku/{}
    Peruutettava haunHakukohteet(String hakuOid, Consumer<List<HakukohdeViiteDTO>> callback, Consumer<Throwable> failureCallback);

    // @GET valintaperusteet-service/resources/valintaperusteet/{}
    @Deprecated
    Peruutettava haeValintaperusteet(String hakukohdeOid, Integer valinnanVaiheJarjestysluku, Consumer<List<ValintaperusteetDTO>> callback, Consumer<Throwable> failureCallback);

    Observable<List<ValintaperusteetDTO>> haeValintaperusteet(String hakukohdeOid, Integer valinnanVaiheJarjestysluku);

    // @GET /valintaperusteet-service/resources/hakukohde/{hakukohdeOid}/ilmanlaskentaa/
    Observable<List<ValinnanVaiheJonoillaDTO>> haeIlmanlaskentaa(String hakukohdeOid);

    // @POST /valintaperusteet-service/resources/valintaperusteet/tuoHakukohde/
    Future<Response> tuoHakukohde(HakukohdeImportDTO hakukohde);

    Observable<List<ValintaperusteDTO>> findAvaimet(String hakukohdeOid);

    Future<List<ValintakoeDTO>> haeValintakokeetHakukohteelle(String hakukohdeOid);

    Peruutettava haeValintakokeetHakukohteelle(String hakukohdeOid, Consumer<List<ValintakoeDTO>> callback, Consumer<Throwable> failureCallback);

    // @POST /valintaperusteet-service/resources/hakukohde/valintakoe
    Future<List<HakukohdeJaValintakoeDTO>> haeValintakokeetHakukohteille(Collection<String> hakukohdeOids);

    Peruutettava haeValintakokeetHakukohteille(Collection<String> hakukohdeOids, Consumer<List<HakukohdeJaValintakoeDTO>> callback, Consumer<Throwable> failureCallback);

    Peruutettava haeValinnanvaiheetHakukohteelle(String hakukohdeOid, Consumer<List<ValinnanVaiheJonoillaDTO>> callback, Consumer<Throwable> failureCallback);

    // @GET /valintaperusteet-service/resources/valinnanvaihe/{oid}/hakukohteet
    Observable<Set<String>> haeHakukohteetValinnanvaiheelle(String oid);
}
