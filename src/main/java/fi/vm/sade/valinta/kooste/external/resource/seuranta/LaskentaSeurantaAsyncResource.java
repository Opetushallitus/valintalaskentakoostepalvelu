package fi.vm.sade.valinta.kooste.external.resource.seuranta;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import fi.vm.sade.valinta.kooste.valintalaskenta.resource.LaskentaParams;
import fi.vm.sade.valinta.seuranta.dto.*;
import rx.Observable;

import javax.ws.rs.core.Response;

public interface LaskentaSeurantaAsyncResource {

    void otaSeuraavaLaskentaTyonAlle(Consumer<String> uuidCallback, Consumer<Throwable> failureCallback);

    void laskenta(String uuid, Consumer<LaskentaDto> callback, Consumer<Throwable> failureCallback);

    void resetoiTilat(String uuid, Consumer<LaskentaDto> callback, Consumer<Throwable> failureCallback);

    void luoLaskenta(LaskentaParams laskentaParams, List<HakukohdeDto> hakukohdeOids, Consumer<TunnisteDto> callback, Consumer<Throwable> failureCallback);

    void merkkaaHakukohteenTila(String uuid, String hakukohdeOid, HakukohdeTila tila, Optional<IlmoitusDto> ilmoitusDtoOptional);

    void merkkaaLaskennanTila(String uuid, LaskentaTila tila, HakukohdeTila hakukohdetila, Optional<IlmoitusDto> ilmoitusDtoOptional);

    Observable<Response> merkkaaLaskennanTila(String uuid, LaskentaTila tila, Optional<IlmoitusDto> ilmoitusDtoOptional);
}
