package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.proxy;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

import java.util.List;

public interface ValintaperusteProxy {
    List<ValintaperusteetTyyppi> haeValintaperusteet(List<HakuparametritTyyppi> hakuparametrit);

    List<ValintaperusteetDTO> haeValintaperusteet(String hakukohdeOid, Integer valinnanvaihe);
}
