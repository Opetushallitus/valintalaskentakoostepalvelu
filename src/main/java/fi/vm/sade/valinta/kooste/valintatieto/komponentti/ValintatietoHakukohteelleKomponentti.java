package fi.vm.sade.valinta.kooste.valintatieto.komponentti;

import java.util.Arrays;
import java.util.List;

import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.valintatiedot.ValintatietoService;
import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;

@Component("valintatietoHakukohteelleKomponentti")
public class ValintatietoHakukohteelleKomponentti {

	private static final Logger LOG = LoggerFactory
			.getLogger(ValintatietoHakukohteelleKomponentti.class);
	private ValintatietoService valintatietoService;

	@Autowired
	public ValintatietoHakukohteelleKomponentti(
			@Qualifier("valintatietoServiceAsAdmin") ValintatietoService valintatietoService) {
		this.valintatietoService = valintatietoService;
	}

	public List<HakemusOsallistuminenTyyppi> valintatiedotHakukohteelle(
			@Property("valintakoeOid") List<String> valintakoeOids,
			@Property("hakukohdeOid") String hakukohdeOid) {

		List<HakemusOsallistuminenTyyppi> osallistujat = valintatietoService
				.haeValintatiedotHakukohteelle(valintakoeOids, hakukohdeOid);
		if (osallistujat == null || osallistujat.isEmpty()) {
			String oids = null;
			if (osallistujat != null) {
				oids = Arrays.toString(osallistujat.toArray());
			}
			LOG.error(
					"Osallistumistietoja ei saatu hakukohteelle({}), kun valintakokeet[{}] oli kyseessa!",
					new Object[] { hakukohdeOid, oids });

			throw new RuntimeException(
					"ValintatietoService palautti tyhjan osallistujajoukon.");
		}
		return osallistujat;
	}
}
