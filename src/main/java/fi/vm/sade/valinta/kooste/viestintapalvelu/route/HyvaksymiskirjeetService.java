package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HyvaksymiskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;

import java.util.List;
import java.util.Optional;

public interface HyvaksymiskirjeetService {
    void hyvaksymiskirjeetHakemuksille(KirjeProsessi prosessi, HyvaksymiskirjeDTO hyvaksymiskirjeDTO, List<String> hakemusOids);

    void jalkiohjauskirjeHakukohteelle(KirjeProsessi prosessi, final HyvaksymiskirjeDTO hyvaksymiskirjeDTO);

    void hyvaksymiskirjeetHakukohteelle(KirjeProsessi prosessi, HyvaksymiskirjeDTO hyvaksymiskirjeDTO);

    void hyvaksymiskirjeetHaulle(String hakuOid, String asiointikieli, SijoittelunTulosProsessi prosessi, String defaultValue);

    void hyvaksymiskirjeetHaulleHakukohteittain(String hakuOid, SijoittelunTulosProsessi prosessi, Optional<String> defaultValue);
}
