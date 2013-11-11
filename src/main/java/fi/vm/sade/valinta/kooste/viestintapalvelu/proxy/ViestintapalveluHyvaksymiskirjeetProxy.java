package fi.vm.sade.valinta.kooste.viestintapalvelu.proxy;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;

import javax.ws.rs.core.Response;

public interface ViestintapalveluHyvaksymiskirjeetProxy {
    Response haeHyvaksymiskirjeet(Kirjeet kirjeet);
}
