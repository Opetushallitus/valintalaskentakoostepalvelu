package fi.vm.sade.valinta.kooste.external.resource.sijoittelu;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
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
    Future<HakukohdeDTO> getLatestHakukohdeBySijoittelu(String hakuOid, String hakukohdeOid);

    @Deprecated
    Observable<HakijaPaginationObject> getKoulutuspaikkalliset(String hakuOid, String hakukohdeOid);

}
