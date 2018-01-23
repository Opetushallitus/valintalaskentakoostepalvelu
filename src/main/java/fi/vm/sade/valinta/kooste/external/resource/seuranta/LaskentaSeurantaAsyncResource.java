package fi.vm.sade.valinta.kooste.external.resource.seuranta;

import fi.vm.sade.valinta.kooste.valintalaskenta.resource.LaskentaParams;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeDto;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.IlmoitusDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.TunnisteDto;
import rx.Observable;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface LaskentaSeurantaAsyncResource {

    Observable<String> otaSeuraavaLaskentaTyonAlle();

    Observable<LaskentaDto> laskenta(String uuid);

    Observable<LaskentaDto> resetoiTilat(String uuid);

    void luoLaskenta(LaskentaParams laskentaParams, List<HakukohdeDto> hakukohdeOids, Consumer<TunnisteDto> callback, Consumer<Throwable> failureCallback);

    void merkkaaHakukohteenTila(String uuid, String hakukohdeOid, HakukohdeTila tila, Optional<IlmoitusDto> ilmoitusDtoOptional);

    void merkkaaLaskennanTila(String uuid, LaskentaTila tila, HakukohdeTila hakukohdetila, Optional<IlmoitusDto> ilmoitusDtoOptional);

    Observable<Response> merkkaaLaskennanTila(String uuid, LaskentaTila tila, Optional<IlmoitusDto> ilmoitusDtoOptional);
}
