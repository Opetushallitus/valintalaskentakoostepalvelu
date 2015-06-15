package fi.vm.sade.valinta.kooste.sijoittelu.route;

import org.apache.camel.Body;
import org.apache.camel.InOnly;

import fi.vm.sade.valinta.kooste.sijoittelu.dto.Sijoittelu;

public interface SijoitteluAktivointiRoute {

    final String SIJOITTELU_REITTI = "direct:sijoittele_haku";

    @InOnly
    void aktivoiSijoittelu(@Body Sijoittelu sijoittelu);
}
