package fi.vm.sade.valinta.kooste.kela.komponentti.impl;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import fi.vm.sade.valinta.kooste.exception.OrganisaatioException;
import fi.vm.sade.valinta.kooste.tarjonta.api.OrganisaatioResource;

@Component
public class OppilaitosKomponentti {

	private static final Logger LOG = LoggerFactory
			.getLogger(OppilaitosKomponentti.class);

	@Autowired
	private OrganisaatioResource organisaatioProxy;

	public String haeOppilaitosnumero(String tarjoajaOid) {
		try {
			OrganisaatioRDTO organisaatio = organisaatioProxy.getOrganisaatioByOID(tarjoajaOid);
			if (organisaatio == null) {
				LOG.error(
						"Oppilaitosnumeroa ei voitu hakea organisaatiolle (null) {}",
						tarjoajaOid);
			} else {
				List<String> visitedOids = new LinkedList<String>();
				while (!organisaatio.getTyypit().contains("Oppilaitos")) {
					visitedOids.add(organisaatio.getOid());
					organisaatio = organisaatioProxy.getOrganisaatioByOID(organisaatio.getParentOid());
					if (organisaatio == null) {
						LOG.error("Organisaatiopalvelu ei palauttanut yhteishaun oppilaitosnumeroa tarjoajalle "
								+ tarjoajaOid);
						return "XXXXX";
					}
					if (visitedOids.contains(organisaatio.getParentOid())) {
						//we should never get here
						throw new OrganisaatioException(
								"Organisaatiopalvelu : circular reference in parentoids {} " + tarjoajaOid);
					}
				}
				if (organisaatio.getOppilaitosKoodi() == null || organisaatio.getOppilaitosKoodi().trim().length() == 0) {
					LOG.error("Organisaatiolla  {} ei ole oppilaitosnumeroa (oppilatoskoodi)",	tarjoajaOid);
					return "XXXXX";
				}
				return organisaatio.getOppilaitosKoodi();
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error(
					"Yhteishaunkoulukoodia ei voitu hakea organisaatiolle {}: Virhe {}",
					new Object[] { tarjoajaOid, e.getMessage() });
		}
		throw new OrganisaatioException(
				"Organisaatiopalvelu ei palauttanut oppilaitosnumeroa tarjoajalle "
						+ tarjoajaOid);
	}

	
	public String haeOppilaitosKoodi(String tarjoajaOid) {
		try {
			OrganisaatioRDTO organisaatio = organisaatioProxy
					.getOrganisaatioByOID(tarjoajaOid);
			if (organisaatio == null) {
				LOG.error(
						"Yhteishaunkoulukoodia ei voitu hakea organisaatiolle {}",
						tarjoajaOid);
			} else {
				if (organisaatio.getYhteishaunKoulukoodi() == null) {
					// new
					// OrganisaatioException("Organisaatio ei palauttanut yhteishaun koulukoodia!");
					LOG.error(
							"Yhteishaunkoulukoodia ei voitu hakea organisaatiolle {}",
							tarjoajaOid);
				} else {
					return organisaatio.getYhteishaunKoulukoodi();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error(
					"Yhteishaunkoulukoodia ei voitu hakea organisaatiolle {}: Virhe {}",
					new Object[] { tarjoajaOid, e.getMessage() });
		}
		throw new OrganisaatioException(
				"Organisaatiopalvelu ei palauttanut yhteishaun koulukoodia tarjoajalle "
						+ tarjoajaOid);
		// }
		// return "0000";
		// }
	}

	// private String haeOppilaitos(String tarjoajaOid,
	// Map<String, String> oppilaitosCache, Set<String> oppilaitosErrorSet) {
	// if (oppilaitosCache.containsKey(tarjoajaOid)) {
	// return oppilaitosCache.get(tarjoajaOid);
	// } else {
	// if (oppilaitosErrorSet.contains(tarjoajaOid)) {
	// LOG.error(
	// "Yhteishaunkoulukoodia ei voitu hakea organisaatiolle {}",
	// tarjoajaOid);
	// } else {
	// try {
	// OrganisaatioRDTO organisaatio = organisaatioProxy
	// .haeOrganisaatio(tarjoajaOid);
	// if (organisaatio == null) {
	// // new
	// //
	// OrganisaatioException("Organisaatio ei palauttanut yhteishaun koulukoodia!");
	// LOG.error(
	// "Yhteishaunkoulukoodia ei voitu hakea organisaatiolle {}",
	// tarjoajaOid);
	// oppilaitosErrorSet.add(tarjoajaOid);
	// } else {
	// if (organisaatio.getYhteishaunKoulukoodi() == null) {
	// // new
	// //
	// OrganisaatioException("Organisaatio ei palauttanut yhteishaun koulukoodia!");
	// LOG.error(
	// "Yhteishaunkoulukoodia ei voitu hakea organisaatiolle {}",
	// tarjoajaOid);
	// oppilaitosErrorSet.add(tarjoajaOid);
	// } else {
	// oppilaitosCache.put(tarjoajaOid,
	// organisaatio.getYhteishaunKoulukoodi());
	// return organisaatio.getYhteishaunKoulukoodi();
	// }
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// LOG.error(
	// "Yhteishaunkoulukoodia ei voitu hakea organisaatiolle {}: Virhe {}",
	// new Object[] { tarjoajaOid, e.getMessage() });
	// //
	// OrganisaatioException("Organisaatio ei palauttanut yhteishaun koulukoodia!");
	// oppilaitosErrorSet.add(tarjoajaOid);
	// }
	// }
	// return "0000";
	// }
	// }
}
