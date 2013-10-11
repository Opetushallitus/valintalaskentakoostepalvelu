package fi.vm.sade.valinta.kooste.tarjonta;

import fi.vm.sade.organisaatio.api.model.types.OrganisaatioDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Proxy organisaation kutsumiseen, seamless retry 10 kertaa!
 */
public interface OrganisaatioProxy {

    OrganisaatioDTO haeOrganisaatio(String tarjoajaOid);
}
