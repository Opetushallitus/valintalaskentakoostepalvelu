package fi.vm.sade.valinta.kooste.external.resource.organisaatio;

import java.util.concurrent.Future;

import javax.ws.rs.core.Response;

import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface OrganisaatioAsyncResource {

	/**
	 * 
	 * @param organisaatioOid
	 *            == tarjoajaOid
	 * @return Organisaation tiedot
	 */
	Future<Response> haeOrganisaatio(String organisaatioOid);

	/**
	 *
	 * @param organisaatioOid
	 * @return esim formaatissa 1.2.246.562.10.xxxx/1.2.246.562.10.xxxx/1.2.246.562.10.xxxx
	 */
	Future<String> haeOrganisaationOidKetju(String organisaatioOid);

}
