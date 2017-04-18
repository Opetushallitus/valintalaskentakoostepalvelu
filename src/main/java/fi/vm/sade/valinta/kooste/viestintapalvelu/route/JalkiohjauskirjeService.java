package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

import java.io.InputStream;
import java.util.List;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HyvaksymiskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.JalkiohjauskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import rx.Observable;

public interface JalkiohjauskirjeService {
    void jalkiohjauskirjeetHakemuksille(KirjeProsessi prosessi, JalkiohjauskirjeDTO jalkiohjauskirjeDTO, List<String> hakemusOids);

    void jalkiohjauskirjeetHaulle(KirjeProsessi prosessi, JalkiohjauskirjeDTO jalkiohjauskirjeDTO);
}
