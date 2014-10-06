package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import java.util.List;

import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component("sijoitteluIlmankoulutuspaikkaaKomponentti")
public class SijoitteluIlmankoulutuspaikkaaKomponentti {

	private SijoitteluResource sijoitteluResource;

	@Autowired
	public SijoitteluIlmankoulutuspaikkaaKomponentti(
			SijoitteluResource sijoitteluResource) {
		this.sijoitteluResource = sijoitteluResource;
	}

	public List<HakijaDTO> ilmankoulutuspaikkaa(
			@Property("hakuOid") String hakuOid,
			@Property("sijoitteluajoId") String sijoitteluajoId) {

		final HakijaPaginationObject result = sijoitteluResource.hakemukset(
				hakuOid, SijoitteluResource.LATEST, null, true, null, null,
				null, null);
		return result.getResults();
	}
}
