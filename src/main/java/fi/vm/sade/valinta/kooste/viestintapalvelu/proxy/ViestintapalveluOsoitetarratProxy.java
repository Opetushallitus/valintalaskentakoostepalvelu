package fi.vm.sade.valinta.kooste.viestintapalvelu.proxy;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;

import javax.ws.rs.core.Response;

public interface ViestintapalveluOsoitetarratProxy {
    Response haeOsoitetarrat(Osoitteet osoitteet);
}
