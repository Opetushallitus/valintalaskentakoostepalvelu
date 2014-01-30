package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import org.apache.camel.Body;
import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Koekutsukirje;

@Component
public class KoekutsukirjeetKomponentti {

	private static final Logger LOG = LoggerFactory
			.getLogger(KoekutsukirjeetKomponentti.class);

	private final HaeOsoiteKomponentti osoiteKomponentti;

	@Autowired
	public KoekutsukirjeetKomponentti(HaeOsoiteKomponentti osoiteKomponentti) {
		this.osoiteKomponentti = osoiteKomponentti;
	}

	public Kirjeet<Koekutsukirje> valmistaKoekutsukirjeet(
			@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
			@Body String letterBodyText) {

		final Kirjeet<Koekutsukirje> kirjeet = new Kirjeet<Koekutsukirje>();
		return kirjeet;
	}
}
