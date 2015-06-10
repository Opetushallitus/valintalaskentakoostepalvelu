package fi.vm.sade.valinta.kooste.external.resource.authentication;

import java.util.List;
import java.util.concurrent.Future;

import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;

public interface HenkiloAsyncResource {
    Future<List<Henkilo>> haeTaiLuoHenkilot(List<HenkiloCreateDTO> henkiloPrototyypit);
}
