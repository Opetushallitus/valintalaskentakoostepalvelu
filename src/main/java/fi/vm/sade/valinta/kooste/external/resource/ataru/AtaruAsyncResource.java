package fi.vm.sade.valinta.kooste.external.resource.ataru;

import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import rx.Observable;

import java.util.List;

public interface AtaruAsyncResource {

    Observable<List<AtaruHakemus>> getApplicationsByHakukohde(String hakukohdeOid);
}
