package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.JalkiohjauskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;

import java.util.List;

public interface JalkiohjauskirjeService {
    void jalkiohjauskirjeetHakemuksille(KirjeProsessi prosessi, JalkiohjauskirjeDTO jalkiohjauskirjeDTO, List<String> hakemusOids);

    void jalkiohjauskirjeetHaulle(KirjeProsessi prosessi, JalkiohjauskirjeDTO jalkiohjauskirjeDTO);
}
