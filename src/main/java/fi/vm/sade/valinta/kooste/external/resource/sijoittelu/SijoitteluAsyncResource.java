package fi.vm.sade.valinta.kooste.external.resource.sijoittelu;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import rx.Observable;

/**
 * @Deprecated Use RX API, Observable<...>
 */
public interface SijoitteluAsyncResource {

    @Deprecated
    Future<HakijaPaginationObject> getKaikkiHakijat(String hakuOid, String hakukohdeOid);

    @Deprecated
    Peruutettava getKoulutuspaikkallisetHakijat(String hakuOid, String hakukohdeOid, Consumer<HakijaPaginationObject> callback, Consumer<Throwable> failureCallback);

    @Deprecated
    Future<HakijaPaginationObject> getHakijatIlmanKoulutuspaikkaa(String hakuOid);

    @Deprecated
    Future<HakukohdeDTO> getLatestHakukohdeBySijoittelu(String hakuOid, String hakukohdeOid);

    @Deprecated
    void getLatestHakukohdeBySijoittelu(String hakuOid, String hakukohdeOid, Consumer<HakukohdeDTO> hakukohde, Consumer<Throwable> poikkeus);

    @Deprecated
    void getLatestHakukohdeBySijoitteluAjoId(String hakuOid, String hakukohdeOid, String sijoitteluAjoId, Consumer<HakukohdeDTO> hakukohde, Consumer<Throwable> poikkeus);

    Observable<HakukohdeDTO> getLatestHakukohdeBySijoittelu(String hakuOid, String sijoitteluAjoId, String hakukohdeOid);

    Observable<HakijaPaginationObject> getKoulutuspaikkalliset(String hakuOid);

    Observable<HakijaPaginationObject> getKoulutuspaikkalliset(String hakuOid, String hakukohdeOid);

    Observable<HakukohdeDTO> getHakukohdeBySijoitteluajoPlainDTO(String hakuOid, String hakukohdeOid);

    Observable<HakukohteenValintatulosUpdateStatuses> muutaHakemuksenTilaa(String hakuOid, String hakukohdeOid, List<Valintatulos> valintatulokset, String selite);

    Observable<HakukohteenValintatulosUpdateStatuses> muutaErillishaunHakemuksenTilaa(String hakuOid, String hakukohdeOid, List<Valintatulos> valintatulokset);
}
