package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.proxy;

import java.util.List;

import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

public interface ValintaperusteProxy {
    List<ValintaperusteetTyyppi> haeValintaperusteet(List<HakuparametritTyyppi> hakuparametrit);
}
