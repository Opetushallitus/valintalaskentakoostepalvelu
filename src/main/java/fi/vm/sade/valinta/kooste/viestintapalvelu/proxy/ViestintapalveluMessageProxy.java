package fi.vm.sade.valinta.kooste.viestintapalvelu.proxy;

import javax.ws.rs.core.Response;

public interface ViestintapalveluMessageProxy {
    Response message(String message);
}
