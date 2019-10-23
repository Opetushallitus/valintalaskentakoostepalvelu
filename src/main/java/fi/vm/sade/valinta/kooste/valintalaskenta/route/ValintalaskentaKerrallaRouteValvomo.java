package fi.vm.sade.valinta.kooste.valintalaskenta.route;

import java.util.List;
import java.util.Optional;

import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;

public interface ValintalaskentaKerrallaRouteValvomo {
    Optional<Laskenta> fetchLaskenta(String uuid);

    List<Laskenta> runningLaskentas();
}
