package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HyvaksymiskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.JalkiohjauskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.model.types.KirjeenVastaanottaja;
import java.util.List;

public interface HyvaksymiskirjeetService {
  ProsessiId hyvaksymiskirjeetHakemuksille(HyvaksymiskirjeDTO hyvaksymiskirjeDTO, List<String> hakemusOids);

  ProsessiId jalkiohjauskirjeetHakemuksille(JalkiohjauskirjeDTO jalkiohjauskirjeDTO, List<String> hakemusOids);

  ProsessiId jalkiohjauskirjeHakukohteelle(HyvaksymiskirjeDTO hyvaksymiskirjeDTO);

  ProsessiId hyvaksymiskirjeetHakukohteelle(HyvaksymiskirjeDTO hyvaksymiskirjeDTO);

  ProsessiId hyvaksymiskirjeetHaulle(HyvaksymiskirjeDTO hyvaksymiskirjeDTO, String asiointikieli,
      KirjeenVastaanottaja kirjeenVastaanottaja);

  ProsessiId jalkiohjauskirjeetHaulle(JalkiohjauskirjeDTO hyvaksymiskirjeDTO,
      KirjeenVastaanottaja kirjeenVastaanottaja);

  ProsessiId hyvaksymiskirjeetHaulleHakukohteittain(HyvaksymiskirjeDTO hyvaksymiskirjeDTO);
}
