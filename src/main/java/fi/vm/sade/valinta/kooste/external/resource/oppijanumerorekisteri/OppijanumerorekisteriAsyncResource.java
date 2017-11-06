package fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri;

import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloCreateDTO;

import java.util.List;
import java.util.concurrent.Future;

public interface OppijanumerorekisteriAsyncResource {
    Future<List<HenkiloPerustietoDto>> haeTaiLuoHenkilot(List<HenkiloCreateDTO> henkiloPrototyypit);
    List<HenkiloPerustietoDto> haeHenkilot(List<String> personOids);
}
