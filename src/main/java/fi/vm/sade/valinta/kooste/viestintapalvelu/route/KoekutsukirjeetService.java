package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

import java.util.Collection;
import java.util.List;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KoekutsuDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;

public interface KoekutsukirjeetService {
    void koekutsukirjeetHakemuksille(KirjeProsessi prosessi, KoekutsuDTO koekutsu, Collection<String> hakemusOids);

    void koekutsukirjeetOsallistujille(KirjeProsessi prosessi, KoekutsuDTO koekutsu, List<String> valintakoeOids);
}
