package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static rx.Observable.*;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.service.ErillishaunVientiService;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.KoekutsukirjeetKomponentti;
/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ErillishaunVientiServiceImpl implements ErillishaunVientiService {

	private static final Logger LOG = LoggerFactory
			.getLogger(ErillishaunVientiServiceImpl.class);
	private final KoekutsukirjeetKomponentti koekutsukirjeetKomponentti;
	private final ApplicationAsyncResource applicationAsyncResource;
	private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
	private final ValintaperusteetValintakoeAsyncResource valintakoeResource;
	private final ValintalaskentaValintakoeAsyncResource osallistumisetResource;

	@Autowired
	public ErillishaunVientiServiceImpl(
			KoekutsukirjeetKomponentti koekutsukirjeetKomponentti,
			ApplicationAsyncResource applicationAsyncResource,
			ViestintapalveluAsyncResource viestintapalveluAsyncResource,
			ValintaperusteetValintakoeAsyncResource valintaperusteetValintakoeAsyncResource,
			ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource) {
		this.koekutsukirjeetKomponentti = koekutsukirjeetKomponentti;
		this.applicationAsyncResource = applicationAsyncResource;
		this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
		this.valintakoeResource = valintaperusteetValintakoeAsyncResource;
		this.osallistumisetResource = valintalaskentaValintakoeAsyncResource;
	}
	
	@Override
	public void vie(ErillishakuDTO erillishaku) {
	}
}
