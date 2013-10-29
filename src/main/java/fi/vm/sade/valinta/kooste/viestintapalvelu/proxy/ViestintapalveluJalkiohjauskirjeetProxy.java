package fi.vm.sade.valinta.kooste.viestintapalvelu.proxy;

import javax.ws.rs.core.Response;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;

public interface ViestintapalveluJalkiohjauskirjeetProxy {
    Response haeJalkiohjauskirjeet(Kirjeet kirjeet);
}
