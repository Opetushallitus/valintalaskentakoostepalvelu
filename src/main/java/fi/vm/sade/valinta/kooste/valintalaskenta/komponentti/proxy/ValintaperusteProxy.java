package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.proxy;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;

import java.util.List;

public interface ValintaperusteProxy {

    List<ValintaperusteetDTO> haeValintaperusteet(String hakukohdeOid, Integer valinnanvaihe);
}
