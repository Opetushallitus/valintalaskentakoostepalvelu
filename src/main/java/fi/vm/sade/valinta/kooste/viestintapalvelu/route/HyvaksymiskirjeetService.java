package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HyvaksymiskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;

import java.util.List;
import java.util.Optional;

public interface HyvaksymiskirjeetService {
    ProsessiId hyvaksymiskirjeetHakemuksille(HyvaksymiskirjeDTO hyvaksymiskirjeDTO, List<String> hakemusOids);

    ProsessiId jalkiohjauskirjeHakukohteelle(HyvaksymiskirjeDTO hyvaksymiskirjeDTO);

    ProsessiId hyvaksymiskirjeetHakukohteelle(HyvaksymiskirjeDTO hyvaksymiskirjeDTO);

    ProsessiId hyvaksymiskirjeetHaulle(HyvaksymiskirjeDTO hyvaksymiskirjeDTO, String asiointikieli);

    ProsessiId hyvaksymiskirjeetHaulleHakukohteittain(HyvaksymiskirjeDTO hyvaksymiskirjeDTO);
}
