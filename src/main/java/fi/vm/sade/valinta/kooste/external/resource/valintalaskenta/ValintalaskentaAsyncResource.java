package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta;

import java.util.List;
import java.util.function.Consumer;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import rx.Observable;

public interface ValintalaskentaAsyncResource {
    Peruutettava lisaaTuloksia(String hakuOid, String hakukohdeOid, String tarjoajaOid, ValinnanvaiheDTO vaihe, Consumer<ValinnanvaiheDTO> callback, Consumer<Throwable> failureCallback);

    Peruutettava laskeJaSijoittele(List<LaskeDTO> lista, Consumer<String> callback, Consumer<Throwable> failureCallback);

    Observable<List<ValintatietoValinnanvaiheDTO>> laskennantulokset(String hakukohdeOid);

    Observable<String> valintakokeet(LaskeDTO laskeDTO);

    Observable<String> laske(LaskeDTO laskeDTO);

    Observable<String> laskeKaikki(LaskeDTO laskeDTO);
}
