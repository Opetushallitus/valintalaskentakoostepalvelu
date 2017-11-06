package fi.vm.sade.valinta.kooste.kela.komponentti;

import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;

public interface HenkilotietoSource {
    HenkiloPerustietoDto getByPersonOid(String oid);
}
