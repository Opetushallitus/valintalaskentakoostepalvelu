package fi.vm.sade.valinta.kooste.viestintapalvelu.proxy;

import javax.ws.rs.core.Response;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;

public interface ViestintapalveluOsoitetarratProxy {
    Response haeOsoitetarrat(Osoitteet osoitteet);
}
