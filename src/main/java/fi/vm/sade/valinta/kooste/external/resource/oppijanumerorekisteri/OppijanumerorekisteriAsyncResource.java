package fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri;

import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import io.reactivex.Observable;

import java.util.List;

public interface OppijanumerorekisteriAsyncResource {
    Observable<List<HenkiloPerustietoDto>> haeTaiLuoHenkilot(List<HenkiloCreateDTO> henkiloPrototyypit);
    Observable<List<HenkiloPerustietoDto>> haeHenkilot(List<String> personOids);
}
