package fi.vm.sade.valinta.kooste.viestintapalvelu.proxy;

import javax.ws.rs.core.Response;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;

public interface ViestintapalveluHyvaksymiskirjeetProxy {
    Response haeHyvaksymiskirjeet(Kirjeet kirjeet);
}
