package fi.vm.sade.valinta.kooste.external.resource.seuranta;

import fi.vm.sade.valinta.kooste.valintalaskenta.resource.LaskentaParams;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeDto;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.IlmoitusDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.TunnisteDto;
import io.reactivex.Observable;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

public interface LaskentaSeurantaAsyncResource {

    Observable<Optional<String>> otaSeuraavaLaskentaTyonAlle();

    Observable<LaskentaDto> laskenta(String uuid);

    Observable<LaskentaDto> resetoiTilat(String uuid);

    Observable<TunnisteDto> luoLaskenta(LaskentaParams laskentaParams, List<HakukohdeDto> hakukohdeOids);

    Observable<Response> merkkaaHakukohteenTila(String uuid, String hakukohdeOid, HakukohdeTila tila, Optional<IlmoitusDto> ilmoitusDtoOptional);

    Observable<Response> merkkaaLaskennanTila(String uuid, LaskentaTila tila, HakukohdeTila hakukohdetila, Optional<IlmoitusDto> ilmoitusDtoOptional);

    Observable<Response> merkkaaLaskennanTila(String uuid, LaskentaTila tila, Optional<IlmoitusDto> ilmoitusDtoOptional);
}
