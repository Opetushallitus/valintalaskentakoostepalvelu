package fi.vm.sade.valinta.kooste.kela.komponentti;

import fi.vm.sade.organisaatio.resource.api.TasoJaLaajuusDTO;

public interface TutkinnontasoSource {
    TasoJaLaajuusDTO getTutkinnontaso(String hakukohdeOid);

    String getKoulutusaste(String hakukohdeOid);
}
