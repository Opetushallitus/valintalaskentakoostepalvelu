package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

import java.util.List;

import org.apache.camel.Property;
import org.springframework.security.core.Authentication;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HyvaksymiskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;

public interface HyvaksymiskirjeetService {
    void hyvaksymiskirjeetHakemuksille(KirjeProsessi prosessi, HyvaksymiskirjeDTO hyvaksymiskirjeDTO, List<String> hakemusOids);

    void jalkiohjauskirjeHakukohteelle(KirjeProsessi prosessi, final HyvaksymiskirjeDTO hyvaksymiskirjeDTO);

    void hyvaksymiskirjeetHakukohteelle(KirjeProsessi prosessi, HyvaksymiskirjeDTO hyvaksymiskirjeDTO);
}
