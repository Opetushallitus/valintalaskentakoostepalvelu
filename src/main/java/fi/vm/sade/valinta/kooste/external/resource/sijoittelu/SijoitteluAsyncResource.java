package fi.vm.sade.valinta.kooste.external.resource.sijoittelu;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import rx.Observable;

public interface SijoitteluAsyncResource {

    Future<HakijaPaginationObject> getKaikkiHakijat(String hakuOid, String hakukohdeOid);

    Peruutettava getKoulutuspaikkallisetHakijat(String hakuOid, String hakukohdeOid, Consumer<HakijaPaginationObject> callback, Consumer<Throwable> failureCallback);

    Future<HakijaPaginationObject> getHakijatIlmanKoulutuspaikkaa(String hakuOid);

    Future<HakukohdeDTO> getLatestHakukohdeBySijoittelu(String hakuOid, String hakukohdeOid);

    void getLatestHakukohdeBySijoittelu(String hakuOid, String hakukohdeOid, Consumer<HakukohdeDTO> hakukohde, Consumer<Throwable> poikkeus);

    void getLatestHakukohdeBySijoitteluAjoId(String hakuOid, String hakukohdeOid, Long sijoitteluAjoId, Consumer<HakukohdeDTO> hakukohde, Consumer<Throwable> poikkeus);

    Observable<HakijaPaginationObject> getKoulutuspaikkalliset(String hakuOid);

    Observable<HakijaPaginationObject> getKoulutuspaikkalliset(String hakuOid, String hakukohdeOid);

    Observable<HakukohdeDTO> getHakukohdeBySijoitteluajoPlainDTO(String hakuOid, String hakukohdeOid);

}
