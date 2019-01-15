package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta;

import fi.vm.sade.valintalaskenta.domain.dto.JonoDto;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import io.reactivex.Observable;

import java.util.List;

public interface ValintalaskentaAsyncResource {
    Observable<List<JonoDto>> jonotSijoitteluun(String hakuOid);

    Observable<ValinnanvaiheDTO> lisaaTuloksia(String hakuOid, String hakukohdeOid, String tarjoajaOid, ValinnanvaiheDTO vaihe);

    Observable<String> laskeJaSijoittele(List<LaskeDTO> lista);

    Observable<List<ValintatietoValinnanvaiheDTO>> laskennantulokset(String hakukohdeOid);

    Observable<String> valintakokeet(LaskeDTO laskeDTO);

    Observable<String> laske(LaskeDTO laskeDTO);

    Observable<String> laskeKaikki(LaskeDTO laskeDTO);
}
