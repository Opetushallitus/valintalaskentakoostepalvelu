package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeRoute;

@Component
public class HyvaksymiskirjeRouteImpl extends AbstractDokumenttiRoute {

	private final ViestintapalveluResource viestintapalveluResource;
	private final HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti;
	private final String hyvaksymiskirjeet;

	@Autowired
	public HyvaksymiskirjeRouteImpl(
			@Value(HyvaksymiskirjeRoute.SEDA_HYVAKSYMISKIRJEET) String hyvaksymiskirjeet,
			ViestintapalveluResource viestintapalveluResource,
			HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti) {
		super();
		this.hyvaksymiskirjeet = hyvaksymiskirjeet;
		this.viestintapalveluResource = viestintapalveluResource;
		this.hyvaksymiskirjeetKomponentti = hyvaksymiskirjeetKomponentti;
	}

	@Override
	public void configure() throws Exception {
		from(hyvaksymiskirjeet)
		// TODO: Hae osoitteet erikseen
		// TODO: Cache ulkopuolisiin palvelukutsuihin
				.bean(hyvaksymiskirjeetKomponentti)
				//
				.bean(viestintapalveluResource, "haeHyvaksymiskirjeet");
	}

}
