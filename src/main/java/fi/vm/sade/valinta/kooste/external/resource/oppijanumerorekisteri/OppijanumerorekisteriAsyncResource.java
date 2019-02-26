package fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri;

import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.util.List;
import java.util.Map;

public interface OppijanumerorekisteriAsyncResource {
    Observable<List<HenkiloPerustietoDto>> haeTaiLuoHenkilot(List<HenkiloCreateDTO> henkiloPrototyypit);
    Single<Map<String, HenkiloPerustietoDto>> haeHenkilot(List<String> personOids);
}
