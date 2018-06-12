package fi.vm.sade.valinta.kooste.external.resource.ataru;

import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import rx.Observable;

import java.util.List;

public interface AtaruAsyncResource {
    Observable<List<HakemusWrapper>> getApplicationsByHakukohde(String hakukohdeOid);

    Observable<List<HakemusWrapper>> getApplicationsByOids(List<String> oids);
}
