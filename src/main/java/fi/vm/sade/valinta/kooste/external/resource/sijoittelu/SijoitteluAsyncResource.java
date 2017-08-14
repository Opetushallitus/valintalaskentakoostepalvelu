package fi.vm.sade.valinta.kooste.external.resource.sijoittelu;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import rx.Observable;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * @Deprecated Use RX API, Observable<...>
 * @Deprecated Pitäisi käyttää Valintarekisteriä
 */
public interface SijoitteluAsyncResource {

    @Deprecated
    Future<HakijaPaginationObject> getKaikkiHakijat(String hakuOid, String hakukohdeOid);

    @Deprecated
    Peruutettava getKoulutuspaikkallisetHakijat(String hakuOid, String hakukohdeOid, Consumer<HakijaPaginationObject> callback, Consumer<Throwable> failureCallback);

    Observable<HakijaPaginationObject> getHakijatIlmanKoulutuspaikkaa(String hakuOid);

    @Deprecated
    Future<HakukohdeDTO> getLatestHakukohdeBySijoittelu(String hakuOid, String hakukohdeOid);

    @Deprecated
    void getLatestHakukohdeBySijoittelu(String hakuOid, String hakukohdeOid, Consumer<HakukohdeDTO> hakukohde, Consumer<Throwable> poikkeus);

    @Deprecated
    Observable<HakukohdeDTO> getLatestHakukohdeBySijoittelu(String hakuOid, String sijoitteluAjoId, String hakukohdeOid);

    @Deprecated
    Observable<HakijaPaginationObject> getKoulutuspaikkalliset(String hakuOid);

    @Deprecated
    Observable<HakijaPaginationObject> getKoulutuspaikkalliset(String hakuOid, String hakukohdeOid);

    @Deprecated
    Observable<HakijaDTO> getHakijaByHakemus(String hakuOid, String hakemusOid);

    @Deprecated
    Observable<HakukohdeDTO> getHakukohdeBySijoitteluajoPlainDTO(String hakuOid, String hakukohdeOid);

    @Deprecated
    Observable<HakukohdeDTO> asetaJononValintaesitysHyvaksytyksi(String hakuOid, String hakukohdeOid, String valintatapajonoOid, Boolean hyvaksytty);

    @Deprecated
    Observable<HakukohteenValintatulosUpdateStatuses> muutaHakemuksenTilaa(String hakuOid, String hakukohdeOid, List<Valintatulos> valintatulokset, String selite);

    @Deprecated
    Observable<HakukohteenValintatulosUpdateStatuses> tarkistaEtteivatValintatuloksetMuuttuneetHakemisenJalkeen(List<Valintatulos> valintatulokset);
}
